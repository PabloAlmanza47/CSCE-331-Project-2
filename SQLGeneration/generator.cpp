#include <iostream>
#include <vector>
#include <ctime>
#include <fstream>
#include "common.h"

using namespace std;

// BOUNDS
#define MIN_AMOUNT_OF_ENTREES 1 // default: 1
#define MAX_AMOUNT_OF_ENTREES 1 // default: 2
#define MIN_AMOUNT_OF_SIDES 0 // default: 1
#define MAX_AMOUNT_OF_SIDES 2 // default: 2
#define MIN_AMOUNT_OF_DRINKS 0 // default: 1
#define MAX_AMOUNT_OF_DRINKS 2 // default: 2

#define MIN_ORDERS_PER_DAY 20 // default: 200
#define MAX_ORDERS_PER_DAY 50 // default: 300

// How many cashiers each panda express has.
#define EMPLOYEES_PER_LOCATION 3 // default: 3

// DAYS
#define LENGTH_IN_DAYS 7 //default: 7 * 52

// Peak days double the number of orders on that day.
#define NUMBER_OF_PEAK_DAYS 2 // default: 2

#define OPENING_TIME_UNIX 3'600 * 7 // opens at 7:00 am
#define CLOSING_TIME_UNIX 3'600 * 21 // closes at 21:00 or 9:00 pm

// DONT TOUCH THESE
const int MIN_BY_CATEGORY[CATEGORY_LENGTH] = {MIN_AMOUNT_OF_ENTREES, MIN_AMOUNT_OF_SIDES, MIN_AMOUNT_OF_DRINKS};
const int MAX_BY_CATEGORY[CATEGORY_LENGTH] = {MAX_AMOUNT_OF_ENTREES, MAX_AMOUNT_OF_SIDES, MAX_AMOUNT_OF_DRINKS};
#define MIN_ITEMS_PER_ORDER MIN_AMOUNT_OF_ENTREES + MIN_AMOUNT_OF_SIDES + MIN_AMOUNT_OF_DRINKS
#define MAX_ITEMS_PER_ORDER MAX_AMOUNT_OF_ENTREES + MAX_AMOUNT_OF_SIDES + MAX_AMOUNT_OF_DRINKS

int main() {
    //The timespan will be LENGTH_IN_DAYS days before now to now.
    time_t startTime = time(nullptr) - 86'400 * LENGTH_IN_DAYS;
    tm* timeinfo = std::localtime(&startTime);
    startTime = startTime - (timeinfo->tm_hour * 3'600 + timeinfo->tm_min * 60 + timeinfo->tm_sec) + OPENING_TIME_UNIX;
    Generator* g = new Generator(startTime, NUMBER_OF_PEAK_DAYS);

    g->generateNDays(LENGTH_IN_DAYS);

    g->writeAll("../SqlCommands/Script.sql");

    cout << "Generation completed successfully!" << endl;

    delete g;

    return 0;
}

//Init currentId before it is used.
    int Scriptable::currentId = 0;

// SQL CONVERT FUNCTIONS

string Location::toSql() const {
    return ""; // TODO
}

string User::toSql() const {
    return ""; // TODO
}

string Item::toSql() const {
    string outp{"INSERT INTO item (item_id, category_id, price, name) VALUES ("};
    outp.reserve(128);

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(category + 1);
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
    string outp{"INSERT INTO receipt (receipt_id, receipt_timestamp) VALUES ("};
    outp.reserve(64 + 192 * items.size());

    outp += std::to_string(id);
    outp += ", '";

    char buf[40];
    std::tm* timepoint = std::localtime(&timestamp);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;
    outp += "');\n";

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

int Location::getId() const noexcept {
    return id;
}

int User::getUserId() const noexcept {
    return userId;
}

int Item::getId() const noexcept {
    return id;
}

float Item::getPrice() const noexcept {
    return price;
}

ItemCategory Item::getCategory() const noexcept {
    return category;
}

void OrderObject::setParentId(int id) {
    parentId = id;
}

Item*& OrderObject::getItem() noexcept {
    return item;
}

int& OrderObject::getItemId() noexcept {
    return itemId;
}

int& OrderObject::getAmount() noexcept {
    return amount;
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
    percentGen = uniform_real_distribution<double>(0.0, 1.0);

    itemsInOrder = uniform_int_distribution<int>(MIN_ITEMS_PER_ORDER, MAX_ITEMS_PER_ORDER);
    itemChoice = uniform_int_distribution<int>(0, allItemsLen - 1);
    ordersPerDay = uniform_int_distribution<int>(MIN_ORDERS_PER_DAY, MAX_ORDERS_PER_DAY);

    cout << "The peak days are on ";
    for (int i = 0, prev = 0; i < peakDaysLen; ++i) {
        if (peakDaysLen > 2 && i != 0 && i != peakDaysLen - 1)
            cout << ", ";
        if (i == peakDaysLen - 1)
            cout << " and ";
        peakDays[i] = std::round(percentGen(rngSeed) * (LENGTH_IN_DAYS - prev - peakDaysLen + i)) + prev;
        if (peakDays[i] == prev)
            ++(peakDays[i]);
        prev = peakDays[i];
        cout << "day " << std::to_string(prev + 1);
    }
    cout << '.' << endl;
}

OrderObject* Generator::generateOrderObject(int counts[]) {

    int itemIndex = itemChoice(rngSeed);
    Item* itemPointer = allItems[itemIndex];
    ItemCategory category = itemPointer->getCategory();

    int max = MAX_BY_CATEGORY[category] - counts[category];

    int amount = max <= 1 ? 1 : std::round(percentGen(rngSeed) * (max - 1)) + 1;

    return new OrderObject(itemPointer->getId(), amount, itemPointer);
}

void Generator::regenerateOrderObject(OrderObject* order, int count, ItemCategory category) {
    int itemId = 1;

    for (int i = 0; i < category; ++i)
        itemId += categoryLens[i];

    itemId += std::round(percentGen(rngSeed) * (categoryLens[category] - 1));

    order->getItemId() = itemId;
    order->getItem() = allItems[itemId - 1];

    int max = MAX_BY_CATEGORY[category] - count;
    order->getAmount() = max <= 1 ? 1 : std::round(percentGen(rngSeed) * (max - 1)) + 1;
}

Order* Generator::generateOrder(time_t durationOfOrder) {

    int numOfItems = itemsInOrder(rngSeed);
    vector<OrderObject*> items;
    items.reserve(numOfItems);

    bool canExit = false;
    int counts[CATEGORY_LENGTH];
    int itemCount = 0;
    for (int i = 0; i < numOfItems || !canExit; ++i) {
        OrderObject* obj = generateOrderObject(counts);

    retry:
        Item* item = obj->getItem();
        ItemCategory category = item->getCategory();

        int itemId = item->getId();
        int count = counts[category];
        int max = MAX_BY_CATEGORY[category];

        if (count >= max) {
            category = static_cast<ItemCategory>(category + 1 == CATEGORY_LENGTH ? 0 : category + 1);
            regenerateOrderObject(obj, counts[category], category);
            goto retry;
        }
        else if (count + obj->getAmount() > max)
            obj->getAmount() = max - count;

        int amount = obj->getAmount();

        // If the amount is above 1, shift the for loop to account for this
        if (amount > 1)
            i += amount - 1;

        itemCount += amount;
        counts[category] += amount;

        //This is O(n^2), but each order will never contain a large amount of items so it should be fine.
        for (size_t j = 0; j < items.size(); ++j) {
            if (items[j]->getItem()->getId() == itemId) {
                ++(items[j]->getAmount());
                delete obj;
                obj = nullptr;
                break;
            }
        }

        if (obj != nullptr)
            items.push_back(obj);

        if (!canExit) {
            bool failed = false;

            for (int i = 0; i < CATEGORY_LENGTH; ++i)
                if (counts[i] < MIN_BY_CATEGORY[i]) {
                    failed = true;
                    break;
                }

            if (!failed)
                canExit = true;
        }
    }

    // Very ugly, but important. Makes sure to evenly remove items if there are more items than allowed.
    if (itemCount > numOfItems) {
        int overAmount = itemCount - numOfItems;
        for (int i = 0; i < CATEGORY_LENGTH && overAmount > 0; ++i) {
            if (counts[i] <= MIN_BY_CATEGORY[i])
                continue;

            for (int j = items.size() - 1; j >= 0 && overAmount > 0; --j) {
                if (items[j]->getItem()->getCategory() != i) 
                    continue;

                if (items[j]->getAmount() > 1)
                    --(items[j]->getAmount());
                else {
                    delete items[j];
                    std::swap(items[j], items.back());
                    items.pop_back();
                }

                --overAmount;
            }                    
        }
    }

    Order* o = new Order(currentLocation->getId(), currentUser->getUserId(), std::move(items), currentDay);
    currentDay += durationOfOrder;
    return o;
}

void Generator::generateNextDay() {

    const int dayLength = CLOSING_TIME_UNIX - OPENING_TIME_UNIX;

    size_t numOfOrders = ordersPerDay(rngSeed);
    int totalOrders = orders.size();

    if (current < peakDaysLen && peakDays[current] <= totalOrders) {
        numOfOrders *= 2;
        ++current;
    }

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

void Generator::writeEnumTypes(ofstream& stream) const {
    stream << "CREATE TYPE user_perm AS ENUM (";

    for (int i = 0; i < USER_PERM_LENGTH; ++i) {
        if (i != 0)
            stream << ", ";
        stream << "'" << to_string(static_cast<UserPerm>(i)) << "'";
    }

    stream << ");\n";
}

void Generator::writeTruncates(ofstream& stream) const {
    stream << "TRUNCATE TABLE category CASCADE;\n";
    stream << "TRUNCATE TABLE item CASCADE;\n";
    stream << "TRUNCATE TABLE receipt CASCADE;\n";
    stream << "TRUNCATE TABLE receipt_item CASCADE;\n";
}

void Generator::writeCategories(ofstream& stream) const {    
    for (int i = 0; i < CATEGORY_LENGTH; ++i)
        stream << "INSERT INTO category (category_id, name) VALUES (" << std::to_string(i + 1) << ", '" << to_string((ItemCategory)i) << "');\n";
}

void Generator::writeItems(ofstream& stream) const {
    for (int i = 0; i < allItemsLen; ++i)
        stream << *(allItems[i]) << '\n';
}

void Generator::writeDays(ofstream& stream) const {
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

        stream << "--Day " << std::to_string(i + 1) << " (" << buf << ") [" << std::to_string(currentOrderLen) << " orders]\n";

        std::strftime(buf, sizeof(buf), "%I:%M %p", timepoint);

        stream << "\n--Order 1 (" << buf << ")\n";
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

void Generator::writeAll(const char* filename) const {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write all: The output file did not open!!" << endl;
        return;
    }

    outputFile << "-- ENUM STATEMENTS\n\n";
    writeEnumTypes(outputFile);
    outputFile << "\n-- TRUNCATE STATEMENTS\n\n";
    writeTruncates(outputFile);
    outputFile << "\n-- CATEGORY GENERATION\n\n";
    writeCategories(outputFile);
    outputFile << "\n-- ITEM GENERATION\n\n";
    writeItems(outputFile);
    outputFile << "\n-- ORDER GENERATION\n\n";
    writeDays(outputFile);

    outputFile.close();
}

// HARDCODED FUNCTIONS

void Generator::generateLocations() {
    const int len = 1;
    allLocations = new Location*[len];
    allLocationsLen = len;

    allLocations[0] = new Location(1, "Polo Garage", "College Station", "Texas");
}

void Generator::generateUsers() {
    int len = allLocationsLen * EMPLOYEES_PER_LOCATION;
    allUsers = new User*[len];
    allUsersLen = len;

    for (int i = 0, id = 1; i < allLocationsLen; ++i)
        for (int j = 0; j < EMPLOYEES_PER_LOCATION; ++j, ++id)
            allUsers[id - 1] = new User(id, "1234", UserPerm::CASHIER);
}

void Generator::generateItems() {//https://www.pandaexpress.com/location/borgen-blvd-sr16/menu/a-la-carte
    const int len = 28;
    allItems = new Item*[len];

    allItems[0] = new Item(1, ItemCategory::ENTREE, 13.80, "Cantonese BBQ Brisket");
    allItems[1] = new Item(2, ItemCategory::ENTREE, 12.30, "Honey Walnut Shrimp");
    allItems[2] = new Item(3, ItemCategory::ENTREE, 12.30, "Black Pepper Sirloin Steak");
    allItems[3] = new Item(4, ItemCategory::ENTREE, 9.30, "Mushroom Chicken");
    allItems[4] = new Item(5, ItemCategory::ENTREE, 9.30, "Kung Pao Chicken");
    allItems[5] = new Item(6, ItemCategory::ENTREE, 9.30, "String Bean Chicken Breast");
    allItems[6] = new Item(7, ItemCategory::ENTREE, 9.30, "The Original Orange Chicken");
    allItems[7] = new Item(8, ItemCategory::ENTREE, 9.30, "SweetFire Chicken Breast");
    allItems[8] = new Item(9, ItemCategory::ENTREE, 9.30, "Honey Sesame Chicken Breast");
    allItems[9] = new Item(10, ItemCategory::ENTREE, 9.30, "Grilled Teriyaki Chicken");
    allItems[10] = new Item(11, ItemCategory::ENTREE, 9.30, "Broccoli Beef");
    allItems[11] = new Item(12, ItemCategory::ENTREE, 9.30, "Beijing Beef");
    allItems[12] = new Item(13, ItemCategory::SIDE, 4.65, "White Steamed Rice");
    allItems[13] = new Item(14, ItemCategory::SIDE, 4.65, "Fried Rice");
    allItems[14] = new Item(15, ItemCategory::SIDE, 4.65, "Chow Mein");
    allItems[15] = new Item(16, ItemCategory::SIDE, 4.65, "Super Greens");
    allItems[16] = new Item(17, ItemCategory::SIDE, 2.10, "Veggie Spring Roll");
    allItems[17] = new Item(18, ItemCategory::SIDE, 2.10, "Chicken Egg Roll");
    allItems[18] = new Item(19, ItemCategory::SIDE, 2.10, "Cream Cheese Rangoon");
    allItems[19] = new Item(20, ItemCategory::DRINK, 3.60, "Watermelon Mango Flavored Refresher");
    allItems[20] = new Item(21, ItemCategory::DRINK, 2.70, "Fanta Orange");
    allItems[21] = new Item(22, ItemCategory::DRINK, 2.70, "Dr Pepper");
    allItems[22] = new Item(23, ItemCategory::DRINK, 2.70, "Dasani");
    allItems[23] = new Item(24, ItemCategory::DRINK, 3.50, "Smartwater");
    allItems[24] = new Item(25, ItemCategory::DRINK, 2.70, "Sprite");
    allItems[25] = new Item(26, ItemCategory::DRINK, 2.70, "Minute Maid Lemonade");
    allItems[26] = new Item(27, ItemCategory::DRINK, 3.60, "Pomegranate Pineapple Flavored Lemonade");
    allItems[27] = new Item(28, ItemCategory::DRINK, 2.70, "Coca Cola Zero Sugar");
    //allItems[2] = new Item(2, , ""));

    for (int i = 0; i < CATEGORY_LENGTH; ++i)
        categoryLens[i] = 0; // gotta init your array :)

    allItemsLen = len;
    for (int i = 0; i < len; ++i)
        ++(categoryLens[allItems[i]->getCategory()]);
}