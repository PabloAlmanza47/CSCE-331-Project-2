#include <iostream>
#include <vector>
#include <ctime>
#include <fstream>
#include "Common.h"

using namespace std;

// BOUNDS
#define MIN_ITEMS_PER_ORDER 1 // default: 1
#define MAX_ITEMS_PER_ORDER 10 // default: 5

#define MIN_AMOUNT_OF_ITEMS 1 // default: 1
#define MAX_AMOUNT_OF_ITEMS 3 // default: 2

#define MIN_ORDERS_PER_DAY 50 // default: 200
#define MAX_ORDERS_PER_DAY 100 // default: 300

//DAYS
#define LENGTH_IN_DAYS 7 * 1

#define OPENING_TIME_UNIX 3'600 * 7 // opens at 7:00 am
#define CLOSING_TIME_UNIX 3'600 * 21 // closes at 21:00 or 9:00 pm

int main() {
    //The timespan will be LENGTH_IN_DAYS days before now to now.
    time_t startTime = time(nullptr) - 86'400 * LENGTH_IN_DAYS;
    tm* timeinfo = std::localtime(&startTime);
    startTime = startTime - (timeinfo->tm_hour * 3'600 + timeinfo->tm_min * 60 + timeinfo->tm_sec) + OPENING_TIME_UNIX;
    Generator* g = new Generator(startTime);

    g->generateNDays(LENGTH_IN_DAYS);

    // g->writeItems("../SqlCommands/Items.sql");
    // g->writeDays("../SqlCommands/Orders.sql");

    g->writeAll("../SqlCommands/Script.sql");

    cout << "Generation completed successfully!" << endl;

    delete g;

    return 0;
}

//Init currentId before it is used.
    int Scriptable::currentId = 0;

// SQL CONVERT FUNCTIONS

string Item::toSql() const {
    string outp{"INSERT INTO item (item_id, price, name) VALUES ("};
    outp.reserve(128);

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(price);
    outp += ", ";
    outp += '\'' + name + '\'';

    return outp + ");";
}

string OrderObject::toSql() const {
    string outp{"INSERT INTO receipt_item (receipt_item_id, receipt_id, item_id, quantity, price_at_sale) VALUES ("};
    outp.reserve(192);

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(parentId);
    outp += ", ";
    outp += std::to_string(itemId);
    outp += ", ";
    outp += std::to_string(amount);
    outp += ", ";
    outp += std::to_string(amount * item->getPrice());
    
    return outp + ");";
}

string Order::toSql() const {
    string outp{"INSERT INTO receipt (receipt_id, timestamp) VALUES ("};
    outp.reserve(64 + 192 * items.size());

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(timestamp);
    outp += ");\n";

    for (size_t i = 0; i < items.size(); ++i)
        outp += '\t' + items[i]->toSql() + '\n';

    return outp;
}

// MISC FUNCTIONS

ostream& operator<<(ostream& os, const Scriptable& obj) {
    os << obj.toSql();
    return os;
}
string Scriptable::toSql() const {
    return "";
}

int Item::getId() const noexcept {
    return id;
}

float Item::getPrice() const noexcept {
    return price;
}

void OrderObject::setParentId(int id) {
    parentId = id;
}

void Order::setChildIds() {
    for (size_t i = 0; i < items.size(); ++i) {
        if (items[i] == nullptr) {
            cout << "WARNING: ITEM #" << std::to_string(i) << " IS NULL!" << endl;
            continue;
        }

        items[i]->setParentId(id);
    }
}

time_t Order::getTimestamp() const noexcept {
    return timestamp;
}

// GENERATOR FUNCTIONS

void Generator::setupRandomGenerators() {
    itemsInOrder = uniform_int_distribution<int>(MIN_ITEMS_PER_ORDER, MAX_ITEMS_PER_ORDER);
    itemChoice = uniform_int_distribution<int>(0, allItems.size() - 1);
    itemAmount = uniform_int_distribution<int>(MIN_AMOUNT_OF_ITEMS, MAX_AMOUNT_OF_ITEMS);
    ordersPerDay = uniform_int_distribution<int>(MIN_ORDERS_PER_DAY, MAX_ORDERS_PER_DAY);
}

OrderObject* Generator::generateOrderObject() {

    int itemIndex = itemChoice(rndSeed);
    Item* itemPointer = allItems[itemIndex];
    int amount = itemAmount(rndSeed);

    return new OrderObject(itemPointer->getId(), amount, itemPointer);
}

Order* Generator::generateOrder(time_t durationOfOrder) {

    size_t numOfItems = itemsInOrder(rndSeed);
    vector<OrderObject*> items;
    items.reserve(numOfItems);

    for (size_t i = 0; i < numOfItems; ++i)
        items.push_back(generateOrderObject());

    Order* o = new Order(std::move(items), currentDay);
    currentDay += durationOfOrder;
    return o;
}

void Generator::generateNextDay() {

    const int dayLength = CLOSING_TIME_UNIX - OPENING_TIME_UNIX;

    size_t numOfOrders = ordersPerDay(rndSeed);
    vector<Order*>* ordersToday = new vector<Order*>();
    ordersToday->reserve(numOfOrders);

    int durationPerOrder = dayLength / numOfOrders;

    for (size_t i = 0; i < numOfOrders; ++i)
        ordersToday->push_back(generateOrder(durationPerOrder));

    int extraTime = 86'400 - durationPerOrder * numOfOrders;
    if (extraTime > 0)
        currentDay += extraTime;

    orders.push_back(ordersToday);
}

void Generator::generateNDays(int days) {
    for (int i = 0; i < days; ++i)
        generateNextDay();
}

void Generator::writeDays(const char* filename) {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write days: The output file did not open!!" << endl;
        return;
    }

    writeDays(outputFile);

    outputFile.close();
}

void Generator::writeDays(ofstream& stream) {

    stream << "TRUNCATE TABLE receiept;\nTRUNCATE TABLE receipt_item;\n\n";

    char buf[40];
    size_t orderLen = orders.size(), currentOrderLen;
    for (size_t i = 0; i < orderLen; ++i) {
        vector<Order*> current = *(orders[i]);
        currentOrderLen = current.size();

        if (i != 0)
            stream << '\n';

        time_t time = current[0]->getTimestamp();
        std::tm* timepoint = std::localtime(&time);
        std::strftime(buf, sizeof(buf), "%d/%m/%Y", timepoint);

        stream << "--Day " << std::to_string(i + 1) << " (" << buf << ')';

        std::strftime(buf, sizeof(buf), "%I:%M %p", timepoint);

        stream << "\n\n--Order 1 (" << buf << ")\n";
        for (size_t j = 0; j < currentOrderLen; ++j) {
            stream << *(current[j]);

            if (j < currentOrderLen - 1) {
                time = current[j + 1]->getTimestamp();
                timepoint = std::localtime(&time);
                std::strftime(buf, sizeof(buf), "%I:%M %p", timepoint);
            }

            if (j != currentOrderLen - 1)
                stream << "\n--Order " << std::to_string(j + 2) << " (" << buf << ')' << '\n';
        }
    }
}

void Generator::writeItems(const char* filename) {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write items: The output file did not open!!" << endl;
        return;
    }

    writeItems(outputFile);

    outputFile.close();
}

void Generator::writeItems(ofstream& stream) {

    stream << "TRUNCATE TABLE item;\n\n";

    size_t itemLen = allItems.size();
    for (size_t i = 0; i < itemLen; ++i)
        stream << *(allItems[i]) << '\n';
}

void Generator::writeAll(const char* filename) {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write all: The output file did not open!!" << endl;
        return;
    }

    outputFile << "-- ITEM GENERATION\n\n";
    writeItems(outputFile);
    outputFile << "\n-- ORDER GENERATION\n\n";
    writeDays(outputFile);

    outputFile.close();
}

// ITEMS

void Generator::generateItems() {//https://www.pandaexpress.com/location/borgen-blvd-sr16/menu/a-la-carte
    allItems.push_back(new Item(1, 13.80, "Cantonese BBQ Brisket"));
    allItems.push_back(new Item(2, 12.30, "Honey Walnut Shrimp"));
    allItems.push_back(new Item(3, 12.30, "Black Pepper Sirloin Steak"));
    allItems.push_back(new Item(4, 9.30, "Mushroom Chicken"));
    allItems.push_back(new Item(5, 9.30, "Kung Pao Chicken"));
    allItems.push_back(new Item(6, 9.30, "String Bean Chicken Breast"));
    allItems.push_back(new Item(7, 9.30, "The Original Orange Chicken"));
    allItems.push_back(new Item(8, 9.30, "SweetFire Chicken Breast"));
    // allItems.push_back(new Item(9, , ""));
    // allItems.push_back(new Item(10, , ""));
    // allItems.push_back(new Item(11, , ""));
    // allItems.push_back(new Item(12, , ""));
    // allItems.push_back(new Item(13, , ""));
    // allItems.push_back(new Item(14, , ""));
    //allItems.push_back(new Item(1, , ""));
}