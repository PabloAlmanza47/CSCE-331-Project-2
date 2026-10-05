#include <iostream>
#include <random>
#include <vector>
#include <algorithm>
#include <ctime>
#include <fstream>
#include "common.h"

using namespace std;

// BOUNDS
#define MIN_AMOUNT_OF_ENTREES 1 // default: 1
#define MAX_AMOUNT_OF_ENTREES 2 // default: 2
#define MIN_AMOUNT_OF_SIDES 1 // default: 1
#define MAX_AMOUNT_OF_SIDES 2 // default: 2
#define MIN_AMOUNT_OF_DRINKS 1 // default: 1
#define MAX_AMOUNT_OF_DRINKS 2 // default: 2

#define MIN_ORDERS_PER_DAY 200 // default: 200
#define MAX_ORDERS_PER_DAY 300 // default: 300

// How many cashiers each panda express has.
#define EMPLOYEES_PER_LOCATION 6 // default: 3

// How many managers each panda express has.
#define MANAGERS_PER_LOCATION 1 // default: 1

// How many hours a cashier works before being rotated out.
#define EMPLOYEE_WORK_SHIFT 7 // default: 7

// Whether or not to make the script somewhat nice looking (false), or fast (true)
#define FAST_MODE true // default: true

// DAYS
#define LENGTH_IN_DAYS 7 * 52 //default: 7 * 52

// Peak days double the number of orders on that day.
#define NUMBER_OF_PEAK_DAYS 2 // default: 2

#define OPENING_TIME_UNIX 3'600 * 7 // opens at 7:00 am
#define CLOSING_TIME_UNIX 3'600 * 21 // closes at 21:00 or 9:00 pm

#define PEAK_LUNCH_TIME_UNIX 3'600 * 13 // peak lunch time is 1:00 pm
#define PEAK_DINNER_TIME_UNIX 3'600 * 18 // peak dinner time is 6:00 pm

// DONT TOUCH THESE
#define WORKSHIFT_SECONDS 3'600 * EMPLOYEE_WORK_SHIFT
const int MIN_BY_CATEGORY[CATEGORY_LENGTH] = {MIN_AMOUNT_OF_ENTREES, MIN_AMOUNT_OF_SIDES, MIN_AMOUNT_OF_DRINKS};
const int MAX_BY_CATEGORY[CATEGORY_LENGTH] = {MAX_AMOUNT_OF_ENTREES, MAX_AMOUNT_OF_SIDES, MAX_AMOUNT_OF_DRINKS};
#define MIN_ITEMS_PER_ORDER MIN_AMOUNT_OF_ENTREES + MIN_AMOUNT_OF_SIDES + MIN_AMOUNT_OF_DRINKS
#define MAX_ITEMS_PER_ORDER MAX_AMOUNT_OF_ENTREES + MAX_AMOUNT_OF_SIDES + MAX_AMOUNT_OF_DRINKS

int main() {
    //The timespan will be LENGTH_IN_DAYS days before now to now.
    time_t startTime = time(nullptr) - 86'400 * LENGTH_IN_DAYS;
    tm* timeinfo = std::localtime(&startTime);
    startTime = startTime - (timeinfo->tm_hour * 3'600 + timeinfo->tm_min * 60 + timeinfo->tm_sec);
    Generator* g = new Generator(startTime, NUMBER_OF_PEAK_DAYS);

    g->generateNDays(LENGTH_IN_DAYS);

    const char* filename = "../SqlCommands/Script.sql";
    if (FAST_MODE)
        g->writeAllFast(filename);
    else
        g->writeAll(filename);

    g->writeTeardown("../SqlCommands/Teardown.sql");

    cout << "Generation completed successfully!" << endl;

    delete g;

    return 0;
}

//Init currentId before it is used.
    int Scriptable::currentId = 0;

// SQL CONVERT FUNCTIONS

string Location::toSql() const noexcept {
    return "INSERT INTO location (location_id, name, city, state)";
}
string Location::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", '";
    outp += name;
    outp += "', '";
    outp += city;
    outp += "', '";
    outp += state;
    outp += '\'';

    return outp + ")";
}

string User::toSql() const noexcept {
    return "INSERT INTO app_user (user_id, name, password, perms)";
}
string User::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(userId);
    outp += ", '";
    outp += name;
    outp += "', '";
    outp += password;
    outp += "', '";
    outp += to_string(perm);
    outp += '\'';


    return outp + ")";
}

string Item::toSql() const noexcept {
    return "INSERT INTO item (item_id, category_id, price, name)";
}
string Item::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(category + 1);
    outp += ", ";
    outp += std::to_string(price);
    outp += ", ";
    outp += '\'' + name + '\'';

    return outp + ")";
}

string Ingredient::toSql() const noexcept {
    return "INSERT INTO inventory (inventory_id, name, stock, min_stock, next_shipment, shelf_life)";
}
string Ingredient::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", '";
    outp += name;
    outp += "', ";
    outp += std::to_string(stock);
    outp += ", ";
    outp += std::to_string(minStock);
    outp += ", '";

    char buf[40];
    std::tm* timepoint = std::localtime(&nextShipment);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;
    outp += "', '";

    timepoint = std::localtime(&shelfLife);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;
    outp += '\'';

    return outp + ')';
}

string IngredientRelation::toSql() const noexcept {
    return "INSERT INTO ingredient_list (item_id, inventory_id)";
}
string IngredientRelation::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(itemId);
    outp += ", ";
    outp += std::to_string(ingredientId);

    return outp + ')';
}

string OrderObject::toSql() const noexcept {
    return "INSERT INTO receipt_item (receipt_item_id, receipt_id, item_id, quantity, price_at_sale)";
}
string OrderObject::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(parentId);
    outp += ", ";
    outp += std::to_string(itemId);
    outp += ", ";
    outp += std::to_string(amount);
    outp += ", ";
    outp += std::to_string(amount * item->getPrice());
    
    return outp + ')';
}

string Order::toSql() const noexcept {
    return "INSERT INTO receipt (receipt_id, location_id, user_id, receipt_timestamp)";
}
string Order::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(locationId);
    outp += ", ";
    outp += std::to_string(userId);
    outp += ", '";

    char buf[40];
    std::tm* timepoint = std::localtime(&timestamp);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;
    outp += "')";

    return outp;
}
string Order::toFullSql() const noexcept {
    string outp{toSql() + " VALUES " + toVals() + ';'};
    outp.reserve(64 + 192 * items.size());

    if (items.empty())
        return outp;

    outp += "\n\t" + items[0]->toSql() + " VALUES\n";

    for (int i = 0, size = items.size(); i < size; ++i)
        outp += "\t\t" + items[i]->toVals() + (i == size - 1 ? ";\n" : ",\n");

    return outp;
}

string Timetable::toSql() const noexcept {
    return "INSERT INTO user_timetable (timetable_id, user_id, location_id, clock_in_time, clock_out_time)";
}
string Timetable::toVals() const noexcept {
    string outp{"("};

    outp += std::to_string(id);
    outp += ", ";
    outp += std::to_string(userId);
    outp += ", ";
    outp += std::to_string(locationId);
    outp += ", '";

    char buf[40];
    std::tm* timepoint = std::localtime(&clockIn);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;
    outp += "', '";

    timepoint = std::localtime(&clockOut);
    std::strftime(buf, sizeof(buf), "%Y-%m-%d %H:%M:%S", timepoint);

    outp += buf;

    return outp + "')";
}

// MISC FUNCTIONS

ostream& operator<<(ostream& os, const Scriptable& obj) {
    os << obj.toFullSql();
    return os;
}
string Scriptable::toSql() const noexcept {
    return "";
}
string Scriptable::toVals() const noexcept {
    return "";
}
string Scriptable::toFullSql() const noexcept {
    return toSql() + " VALUES " + toVals() + ';';
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

const vector<OrderObject*>& Order::getItems() const noexcept {
    return items;
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

    lunchOrderTimes = normal_distribution<float>(PEAK_LUNCH_TIME_UNIX, 3'600 * 2);
    dinnerOrderTimes = normal_distribution<float>(PEAK_DINNER_TIME_UNIX, 3'600 * 1);
    orderChoice = bernoulli_distribution(0.65f);

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

Order* Generator::generateOrder(time_t timeOfOrder) {

    int numOfItems = itemsInOrder(rngSeed);
    vector<OrderObject*> items;
    items.reserve(numOfItems);

    bool canExit = false;
    int counts[CATEGORY_LENGTH]{};
    int itemCount = 0;
    for (int i = 0; i < numOfItems || !canExit; ++i) {
        OrderObject* obj = generateOrderObject(counts);
        int retries = 0;

    retry:
        Item* item = obj->getItem();
        ItemCategory category = item->getCategory();

        int itemId = item->getId();
        int count = counts[category];
        int max = MAX_BY_CATEGORY[category];

        if (count >= max) {
            category = static_cast<ItemCategory>(category + 1 == CATEGORY_LENGTH ? 0 : category + 1);
            regenerateOrderObject(obj, counts[category], category);

            if (++retries >= CATEGORY_LENGTH) {
                delete obj;
                break;
            }

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

    time_t timestamp = timeOfOrder;
    Order* o = new Order(currentUser->getUserId(), currentLocation->getId(), std::move(items), timestamp);

    if (timestamp > clockOutTime) {
        userTimetables.push_back(new Timetable(currentUser->getUserId(), currentLocation->getId(), clockInTime, clockOutTime));
        clockInTime = clockOutTime;
        clockOutTime += WORKSHIFT_SECONDS; 
        currentUser = currentUser->getUserId() == allUsersLen ? allUsers[0] : allUsers[currentUser->getUserId()];
    }

    return o;
}

void Generator::generateNextDay() {

    // const int dayLength = CLOSING_TIME_UNIX - OPENING_TIME_UNIX;

    size_t numOfOrders = ordersPerDay(rngSeed);
    int totalOrders = orders.size();

    if (current < peakDaysLen && peakDays[current] <= totalOrders) {
        numOfOrders *= 2;
        ++current;
    }

    vector<Order*>* ordersToday = new vector<Order*>();
    ordersToday->reserve(numOfOrders);

    time_t* orderTimes = new time_t[numOfOrders];

    for (size_t i = 0; i < numOfOrders; ++i)
        orderTimes[i] = min(max((int)round(orderChoice(rngSeed) ? lunchOrderTimes(rngSeed) : dinnerOrderTimes(rngSeed)), OPENING_TIME_UNIX), CLOSING_TIME_UNIX) + currentDay;

    std::sort(orderTimes, orderTimes + numOfOrders);

    clockInTime = currentDay + OPENING_TIME_UNIX;
    clockOutTime = currentDay + WORKSHIFT_SECONDS + OPENING_TIME_UNIX;

    for (size_t i = 0; i < numOfOrders; ++i)
        ordersToday->push_back(generateOrder(orderTimes[i]));
    
    delete[] orderTimes;

    // 30 min = 1,800 seconds
    if (currentDay - clockInTime >= 1'800) {
        userTimetables.push_back(new Timetable(currentUser->getUserId(), currentLocation->getId(), clockInTime, clockOutTime));
        currentUser = currentUser->getUserId() == allUsersLen ? allUsers[0] : allUsers[currentUser->getUserId()];
    }

    currentDay += 86'400;

    orders.push_back(ordersToday);
}

void Generator::generateNDays(int days) {
    for (int i = 0; i < days; ++i)
        generateNextDay();
}

void Generator::writeFast(ofstream& stream, Scriptable** arr, int arrLen) const {
    if (arrLen == 0)
        return;

    stream << arr[0]->toSql() << " VALUES ";

    for (int i = 0; i < arrLen; ++i)
        stream << arr[i]->toVals() << (i == arrLen - 1 ? ';' : ',');
}

void Generator::writeTruncates(ofstream& stream) const {
    stream << "TRUNCATE TABLE category CASCADE;\n";
    stream << "TRUNCATE TABLE item CASCADE;\n";
    stream << "TRUNCATE TABLE inventory CASCADE;\n";
    stream << "TRUNCATE TABLE ingredient_list CASCADE;\n";
    stream << "TRUNCATE TABLE receipt CASCADE;\n";
    stream << "TRUNCATE TABLE receipt_item CASCADE;\n";
    stream << "TRUNCATE TABLE location CASCADE;\n";
    stream << "TRUNCATE TABLE app_user CASCADE;\n";
    stream << "TRUNCATE TABLE user_timetable CASCADE;\n";
}
void Generator::writeTruncatesFast(ofstream& stream) const {
    stream << "TRUNCATE TABLE category,item,inventory,ingredient_list,receipt,receipt_item,location,app_user,user_timetable;";
}


void Generator::writeCategories(ofstream& stream) const {    
    for (int i = 0; i < CATEGORY_LENGTH; ++i)
        stream << "INSERT INTO category (category_id, name) VALUES (" << std::to_string(i + 1) << ", '" << to_string((ItemCategory)i) << "');\n";
}
void Generator::writeCategoriesFast(ofstream& stream) const {
    stream << "INSERT INTO category (category_id, name) VALUES\n";
    for (int i = 0; i < CATEGORY_LENGTH; ++i)
        stream << '(' << std::to_string(i + 1) << ", '" << to_string((ItemCategory)i) << (i == CATEGORY_LENGTH - 1 ? "');" : "'),"); 
}

void Generator::writeItems(ofstream& stream) const {
    for (int i = 0; i < allItemsLen; ++i)
        stream << *(allItems[i]) << '\n';
}
void Generator::writeItemsFast(ofstream& stream) const {
    writeFast(stream, (Scriptable**)allItems, allItemsLen);
}

void Generator::writeInventory(ofstream& stream) const {
    for (int i = 0; i < allIngredientsLen; ++i)
        stream << *(allIngredients[i]) << '\n';
}
void Generator::writeInventoryFast(ofstream& stream) const {
    writeFast(stream, (Scriptable**)allIngredients, allIngredientsLen);
}

void Generator::writeInventoryRelations(ofstream& stream) const {
    for (int i = 0; i < allRelationsLen; ++i)
        stream << *(allRelations[i]) << '\n';
}
void Generator::writeInventoryRelationsFast(ofstream& stream) const {
    writeFast(stream, (Scriptable**)allRelations, allRelationsLen);
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
void Generator::writeDaysFast(ofstream& stream) const {
    size_t size = orders.size(), currentOrderLen;
    stream << (*(orders[0]))[0]->toSql() << " VALUES ";
    for (size_t i = 0; i < size; ++i) {
        vector<Order*> current = *(orders[i]);
        currentOrderLen = current.size();
        bool last = i == size - 1;

        for (size_t j = 0; j < currentOrderLen; ++j) {
            stream << current[j]->toVals() << (last && j == currentOrderLen - 1 ? ';' : ',');
        }
    }

    stream << '\n' << OrderObject::getSql() << " VALUES ";
    for (size_t i = 0; i < size; ++i) {
        vector<Order*> current = *(orders[i]);
        currentOrderLen = current.size();
        bool last1 = i == size - 1;

        for (size_t j = 0; j < currentOrderLen; ++j) {
            const vector<OrderObject*>& arr = current[j]->getItems();
            bool last2 = j == currentOrderLen - 1;
            for (size_t k = 0; k < arr.size(); ++k)
                stream << arr[k]->toVals() << (last1 && last2 && k == arr.size() - 1 ? ';' : ',');
        }
    }
}

void Generator::writeLocations(ofstream& stream) const {
    for (int i = 0; i < allLocationsLen; ++i)
        stream << *(allLocations[i]) << '\n';
}
void Generator::writeLocationsFast(ofstream& stream) const {
    writeFast(stream, (Scriptable**)allLocations, allLocationsLen);
}

void Generator::writeUsers(ofstream& stream) const {
    for (int i = 0; i < allUsersLen; ++i)
        stream << *(allUsers[i]) << '\n';
}
void Generator::writeUsersFast(ofstream& stream) const {
    writeFast(stream, (Scriptable**)allUsers, allUsersLen);
}

void Generator::writeTimetables(ofstream& stream) const {
    for (int i = 0, size = userTimetables.size(); i < size; ++i)
        stream << *(userTimetables[i]) << '\n';
}
void Generator::writeTimetablesFast(ofstream& stream) const {
    Timetable* const* point = (userTimetables.data());
    writeFast(stream, (Scriptable**)point, userTimetables.size());
}

void Generator::writeTeardown(const char* filename) const {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write teardown: The output file did not open!!" << endl;
        return;
    }

    outputFile << "-- Drop tables if they already exist\n";
    outputFile << "DROP TABLE IF EXISTS receipt_item;\n";
    outputFile << "DROP TABLE IF EXISTS receipt;\n";
    outputFile << "DROP TABLE IF EXISTS ingredient_list;\n";
    outputFile << "DROP TABLE IF EXISTS item;\n";
    outputFile << "DROP TABLE IF EXISTS inventory;\n";
    outputFile << "DROP TABLE IF EXISTS user_timetable;\n";
    outputFile << "DROP TABLE IF EXISTS app_user;\n";
    outputFile << "DROP TYPE IF EXISTS user_perm;\n";
    outputFile << "DROP TABLE IF EXISTS category;\n";
    outputFile << "DROP TABLE IF EXISTS location;\n";

    outputFile.close();
}

void Generator::writeAll(const char* filename) const {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write all: The output file did not open!!" << endl;
        return;
    }

    outputFile << "-- TRUNCATE STATEMENTS\n\n";
    writeTruncates(outputFile);
    outputFile << "\n-- LOCATION GENERATION\n\n";
    writeLocations(outputFile);
    outputFile << "\n-- USER GENERATION\n\n";
    writeUsers(outputFile);
    outputFile << "\n-- CATEGORY GENERATION\n\n";
    writeCategories(outputFile);
    outputFile << "\n-- ITEM GENERATION\n\n";
    writeItems(outputFile);
    outputFile << "\n-- INVENTORY GENERATION\n\n";
    writeInventory(outputFile);
    outputFile << "\n-- INVENTORY RELATIONS GENERATION\n\n";
    writeInventoryRelations(outputFile);
    outputFile << "\n-- ORDER GENERATION\n\n";
    writeDays(outputFile);
    outputFile << "\n-- TIMETABLE GENERATION\n\n";
    writeTimetables(outputFile);

    outputFile.close();
}
void Generator::writeAllFast(const char* filename) const {
    ofstream outputFile = ofstream(filename);

    if (!outputFile.is_open()) {
        cerr << "Failed to write all fast: The output file did not open!!" << endl;
        return;
    }

    writeTruncatesFast(outputFile);
    writeLocationsFast(outputFile);
    writeUsersFast(outputFile);
    writeCategoriesFast(outputFile);
    writeItemsFast(outputFile);
    writeInventoryFast(outputFile);
    writeInventoryRelationsFast(outputFile);
    writeDaysFast(outputFile);
    writeTimetablesFast(outputFile);

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
    const int totalEmployees = (EMPLOYEES_PER_LOCATION + MANAGERS_PER_LOCATION) * allLocationsLen; 
    allUsers = new User*[totalEmployees];
    allUsersLen = totalEmployees;

    const char fnameFile[] = "firstname.txt";
    const char lnameFile[] = "lastname.txt";
    const char passwordFile[] = "passwords.txt";
    const int fnameLen = 38'407;
    const int lnameLen = 151'670;
    const int passwordLen = 9'998;

    ifstream firstNames = ifstream(fnameFile);
    ifstream lastNames = ifstream(lnameFile);
    ifstream passwordStream = ifstream(passwordFile);

    if (!firstNames.is_open() || !lastNames.is_open()) {
        cerr << "Issue open name files!!!" << endl;
        return;
    }

    int* fnameLines = new int[totalEmployees];
    int* lnameLines = new int[totalEmployees];
    int* passwordLines = new int[totalEmployees];

    for (int i = 0; i < totalEmployees; ++i) {
        fnameLines[i] = (int)std::round(percentGen(rngSeed) * (fnameLen - 1));
        lnameLines[i] = (int)std::round(percentGen(rngSeed) * (lnameLen - 1));
        passwordLines[i] = (int)std::round(percentGen(rngSeed) * (passwordLen - 1));
        // cout << '(' << to_string(fnameLines[i]) << ", " << to_string(lnameLines[i]) << "), ";
    }
    cout << endl;

    std::sort(fnameLines, fnameLines + totalEmployees);
    std::sort(lnameLines, lnameLines + totalEmployees);

    string* names = new string[totalEmployees];
    string* passwords = new string[totalEmployees];
    string line; 

    for (int currentLine = 0, i = 0, nextLine = fnameLines[0]; std::getline(firstNames, line) && i < totalEmployees; ++currentLine) {
        if (currentLine >= nextLine) {
            names[i] = line + ' ';
            if (++i < totalEmployees)
                nextLine = fnameLines[i];
            else break;
        }
    }
    firstNames.close();

    for (int currentLine = 0, i = 0, nextLine = lnameLines[0]; std::getline(lastNames, line) && i < totalEmployees; ++currentLine) {
        if (currentLine >= nextLine) {
            names[i] += line;
            if (++i < totalEmployees)
                nextLine = lnameLines[i];
            else break;
        }
    }
    lastNames.close();

    for (int currentLine = 0, i = 0, nextLine = passwordLines[0]; std::getline(passwordStream, line) && i < totalEmployees; ++currentLine) {
        if (currentLine >= nextLine) {
            passwords[i] = line;
            if (++i < totalEmployees)
                nextLine = passwordLines[i];
            else break;
        }
    }
    passwordStream.close();

    delete[] fnameLines;
    delete[] lnameLines;
    delete[] passwordLines;

    for (int i = 0, id = 1; i < allLocationsLen; ++i) {
        for (int j = 0; j < EMPLOYEES_PER_LOCATION; ++j, ++id)
            allUsers[id - 1] = new User(id, names[id - 1], passwords[id - 1], UserPerm::CASHIER); 
        for (int j = 0; j < MANAGERS_PER_LOCATION; ++j, ++id)
            allUsers[id - 1] = new User(id, names[id - 1], passwords[id - 1], UserPerm::MANAGER);   
    }

    delete[] names;
    delete[] passwords;

    clockOutTime = currentDay + WORKSHIFT_SECONDS + OPENING_TIME_UNIX;
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

void Generator::generateIngredients(time_t startTime) {
    const int len = 28;
    allIngredients = new Ingredient*[len];
    allIngredientsLen = len;

    allIngredients[0] = new Ingredient(1, "Chicken", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[1] = new Ingredient(2, "BBQ Sauce", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[2] = new Ingredient(3, "Beef", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[3] = new Ingredient(4, "Shrimp", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[4] = new Ingredient(5, "Honey Sauce", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[5] = new Ingredient(6, "Walnuts", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[6] = new Ingredient(7, "Black Pepper", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[7] = new Ingredient(8, "Vegetables", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[8] = new Ingredient(9, "Mushrooms", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[9] = new Ingredient(10, "Spices", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[10] = new Ingredient(11, "Assorted Nuts", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[11] = new Ingredient(12, "Peppers", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[12] = new Ingredient(13, "Soy Sauce", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[13] = new Ingredient(14, "Orange Sauce", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[14] = new Ingredient(15, "White Rice", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[15] = new Ingredient(16, "Noodles", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[16] = new Ingredient(17, "Cream Cheese", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[17] = new Ingredient(18, "Watermelon", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[18] = new Ingredient(19, "Mango", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[19] = new Ingredient(20, "Pomegranate", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[20] = new Ingredient(21, "Pineapple", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[21] = new Ingredient(22, "Fanta Orange", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[22] = new Ingredient(23, "Dr Pepper", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[23] = new Ingredient(24, "Dasani", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[24] = new Ingredient(25, "Smartwater", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[25] = new Ingredient(26, "Sprite", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[26] = new Ingredient(27, "Minute Maid Lemonade", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    allIngredients[27] = new Ingredient(28, "Coca Cola Zero Sugar", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);
    //allIngredients[2] = new Ingredient(2, "", 50, 25, startTime + 86'400 * 7, startTime + 86'400 * 14);

}

void Generator::generateIngredientRelations() {
    const int len = 67;
    allRelations = new IngredientRelation*[len];
    allRelationsLen = len;

    allRelations[0] = new IngredientRelation(1, 2);
    allRelations[1] = new IngredientRelation(1, 3);
    allRelations[2] = new IngredientRelation(2, 4);
    allRelations[3] = new IngredientRelation(2, 5);
    allRelations[4] = new IngredientRelation(2, 6);
    allRelations[5] = new IngredientRelation(3, 7);
    allRelations[6] = new IngredientRelation(3, 8);
    allRelations[7] = new IngredientRelation(3, 3);
    allRelations[8] = new IngredientRelation(4, 1);
    allRelations[9] = new IngredientRelation(4, 9);
    allRelations[10] = new IngredientRelation(4, 8);
    allRelations[11] = new IngredientRelation(5, 8);
    allRelations[12] = new IngredientRelation(5, 10);
    allRelations[13] = new IngredientRelation(5, 11);
    allRelations[14] = new IngredientRelation(5, 12);
    allRelations[15] = new IngredientRelation(6, 1);
    allRelations[16] = new IngredientRelation(6, 8);
    allRelations[17] = new IngredientRelation(6, 13);
    allRelations[18] = new IngredientRelation(7, 1);
    allRelations[19] = new IngredientRelation(7, 14);
    allRelations[20] = new IngredientRelation(8, 1);
    allRelations[21] = new IngredientRelation(8, 8);
    allRelations[22] = new IngredientRelation(8, 12);
    allRelations[23] = new IngredientRelation(9, 1);
    allRelations[24] = new IngredientRelation(9, 8);
    allRelations[25] = new IngredientRelation(9, 10);
    allRelations[26] = new IngredientRelation(10, 1);
    allRelations[27] = new IngredientRelation(10, 10);
    allRelations[28] = new IngredientRelation(11, 3);
    allRelations[29] = new IngredientRelation(11, 8);
    allRelations[30] = new IngredientRelation(11, 13);
    allRelations[31] = new IngredientRelation(11, 10);
    allRelations[32] = new IngredientRelation(12, 3);
    allRelations[33] = new IngredientRelation(12, 13);
    allRelations[34] = new IngredientRelation(12, 10);
    allRelations[35] = new IngredientRelation(12, 12);
    allRelations[36] = new IngredientRelation(13, 15);
    allRelations[37] = new IngredientRelation(14, 15);
    allRelations[38] = new IngredientRelation(14, 8);
    allRelations[39] = new IngredientRelation(14, 13);
    allRelations[40] = new IngredientRelation(15, 16);
    allRelations[41] = new IngredientRelation(15, 13);
    allRelations[42] = new IngredientRelation(15, 8);
    allRelations[43] = new IngredientRelation(15, 10);
    allRelations[44] = new IngredientRelation(16, 8);
    allRelations[45] = new IngredientRelation(16, 10);
    allRelations[46] = new IngredientRelation(17, 8);
    allRelations[47] = new IngredientRelation(17, 10);
    allRelations[48] = new IngredientRelation(17, 12);
    allRelations[49] = new IngredientRelation(18, 1);
    allRelations[50] = new IngredientRelation(18, 8);
    allRelations[51] = new IngredientRelation(18, 10);
    allRelations[52] = new IngredientRelation(18, 13);
    allRelations[53] = new IngredientRelation(19, 17);
    allRelations[54] = new IngredientRelation(19, 10);
    allRelations[55] = new IngredientRelation(19, 8);
    allRelations[56] = new IngredientRelation(20, 18);
    allRelations[57] = new IngredientRelation(20, 19);
    allRelations[58] = new IngredientRelation(21,22);
    allRelations[59] = new IngredientRelation(22,23);
    allRelations[60] = new IngredientRelation(23,24);
    allRelations[61] = new IngredientRelation(24,25);
    allRelations[62] = new IngredientRelation(25, 26);
    allRelations[63] = new IngredientRelation(26, 27);
    allRelations[64] = new IngredientRelation(27, 20);
    allRelations[65] = new IngredientRelation(27, 21);
    allRelations[66] = new IngredientRelation(28, 28);
    //allRelations[6] = new IngredientRelation();
}