#include <iostream>
#include <memory>
#include <string>
#include <vector>
#include <ctime>
#include <random>

// ENUM LENGTHS (no easy way to dynamically get this)
#define CATEGORY_LENGTH 3
#define USER_PERM_LENGTH 2

using namespace std;

class Scriptable {
    public:
        static int currentId;
        
        Scriptable() {
            currentId++;
        }

        int getId() {
            return currentId;
        }

        virtual string toSql() const noexcept;
        virtual string toVals() const noexcept;
        virtual string toFullSql() const noexcept;

        friend ostream& operator<<(ostream& os, const Scriptable& obj);

        virtual ~Scriptable() = default;
};

enum ItemCategory {
    ENTREE, SIDE, DRINK
};

constexpr string_view to_string(ItemCategory category) {
    switch (category) {
        case ENTREE: return "Entree";
        case SIDE: return "Side";
        case DRINK: return "Drink";
        default: return "Unknown";
    }
}

class Location : public Scriptable {
    private:
        int id;
        string name;
        string city;
        string state;

    public:
        Location(int id, string name, string city, string state) : id(id), name(name), city(city), state(state) {}
        Location(string name, string city, string state) : id(getId()), name(name), city(city), state(state) {}

        string toSql() const noexcept override;
        string toVals() const noexcept override;

        int getId() const noexcept;
};

enum UserPerm {
    CASHIER, MANAGER
};

constexpr string_view to_string(UserPerm perms) {
    switch (perms) {
        case CASHIER: return "Cashier";
        case MANAGER: return "Manager";
        default: return "Unknown";
    }
}

class User : public Scriptable {
    private:
        int userId;
        string name;
        string password;
        UserPerm perm;

    public:
        User(int userId, string name, string password, UserPerm perm) : userId(userId), name(name), password(password), perm(perm) {}

        string toSql() const noexcept override;
        string toVals() const noexcept override;

        int getUserId() const noexcept;
};

class Timetable : public Scriptable {
    private:
        int id;
        int userId;
        int locationId;
        time_t clockIn;
        time_t clockOut;

    public:
        Timetable(int id, int userId, int locationId, time_t clockIn, time_t clockOut) : id(id), userId(userId), locationId(locationId), clockIn(clockIn), clockOut(clockOut) {}
        Timetable(int userId, int locationId, time_t clockIn, time_t clockOut) : id(getId()), userId(userId), locationId(locationId), clockIn(clockIn), clockOut(clockOut) {}

        string toSql() const noexcept override;
        string toVals() const noexcept override;
};

class Ingredient : public Scriptable {
    private:
        int id;
        string name;
        int stock;
        int minStock;
        time_t nextShipment;
        time_t shelfLife;

    public:
        Ingredient(int id, string name, int stock, int minStock, time_t nextShipment, time_t shelfLife) : id(id), name(name), stock(stock), minStock(minStock), nextShipment(nextShipment), shelfLife(shelfLife) {}

        string toSql() const noexcept override;
        string toVals() const noexcept override;

};

class Item : public Scriptable {
    private:
        int id;
        ItemCategory category;
        float price;
        string name;

    public:
        Item(int id, ItemCategory category, float price, const string& name) : id(id), category(category), price(price), name(name) {}
        Item(ItemCategory category, float price, const string& name) : id(getId()), category(category), price(price), name(name) {}
        Item(Item& copy) {
            id = copy.id;
            category = copy.category;
            price = copy.price;
            name = string(copy.name.c_str());
        }
        Item(Item&& move) {
            id = move.id;
            category = move.category;
            price = move.price;
            name = std::move(move.name);
        }

        string toSql() const noexcept override;
        string toVals() const noexcept override;

        int getId() const noexcept;
        float getPrice() const noexcept;
        ItemCategory getCategory() const noexcept;
};

struct IngredientRelation : public Scriptable {
    public:
        int itemId;
        int ingredientId;

        IngredientRelation(int itemId, int ingredientId) : itemId(itemId), ingredientId(ingredientId) {}

        string toSql() const noexcept override;
        string toVals() const noexcept override;
};

class OrderObject : public Scriptable {
    private:
        int id;
        int parentId;
        int itemId;
        int amount;
        Item* item;

    public:
        OrderObject(int orderObjId, int itemId, int amount, Item* item) {
            id = orderObjId;
            parentId = -1;
            this->itemId = itemId;
            this->amount = amount;
            this->item = item;
        }
        OrderObject(int itemId, int amount, Item* item) {
            id = getId();
            parentId = -1;
            this->itemId = itemId;
            this->amount = amount;
            this->item = item;
        }
        OrderObject(OrderObject& copy) {
            id = copy.id;
            parentId = copy.parentId;
            itemId = copy.itemId;
            amount = copy.amount;
            item = new Item(*copy.item);
        }
        OrderObject(OrderObject&& move) {
            id = move.id;
            parentId = move.parentId;
            itemId = move.itemId;
            amount = move.amount;
            item = move.item;
        }

        string toSql() const noexcept override;
        string toVals() const noexcept override;
        void setParentId(int id);

        Item*& getItem() noexcept;
        int& getItemId() noexcept;
        int& getAmount() noexcept;

        static string getSql() noexcept {
            return OrderObject(-1,-1, -1, nullptr).toSql();
        }
};

class Order : public Scriptable {
    private:
        int id;
        int userId;
        int locationId;
        vector<unique_ptr<OrderObject>> items;
        time_t timestamp;

    public:
        Order(int id, int userId, int locationId, vector<unique_ptr<OrderObject>>&& items, const time_t& timestamp) : id(id), userId(userId), locationId(locationId), items(std::move(items)), timestamp(timestamp) {
            setChildIds();
        }
        Order(int userId, int locationId, vector<unique_ptr<OrderObject>>&& items, const time_t& timestamp) : id(getId()), userId(userId), locationId(locationId), items(std::move(items)), timestamp(timestamp) {
            setChildIds();
        }

        string toSql() const noexcept override;
        string toVals() const noexcept override;
        string toFullSql() const noexcept override;
        void setChildIds();
        time_t getTimestamp() const noexcept;
        const vector<unique_ptr<OrderObject>>& getItems() const noexcept;

        ~Order() override = default;
};

class Generator {
    private:
        // all item pointers, in an array. Sorted by category.
        vector<unique_ptr<Item>> allItems;
        int allItemsLen = 0;

        // all ingredient points, in an array.
        vector<unique_ptr<Ingredient>> allIngredients;
        int allIngredientsLen = 0;

        // all ingredient relations, in an array.
        vector<unique_ptr<IngredientRelation>> allRelations;
        int allRelationsLen = 0;

        // How many items are in each category.
        int categoryLens[CATEGORY_LENGTH];

        // which days are peak days.
        vector<int> peakDays;
        // when the next peak day is (if this equals peakDaysLen then there are no more peak days).
        int current;
        // length of the peakDays array.
        int peakDaysLen;

        // all used panda express locations 
        vector<unique_ptr<Location>> allLocations;
        int allLocationsLen = 0;

        // all employees
        vector<unique_ptr<User>> allUsers;
        int allUsersLen = 0;

        // all employee time records
        vector<unique_ptr<Timetable>> userTimetables;

        // Every order placed
        vector<vector<unique_ptr<Order>>> orders;

        // The current location and employee
        Location* currentLocation = nullptr;
        User* currentUser = nullptr;

        // The current day
        time_t currentDay;
        time_t clockInTime;
        time_t clockOutTime;

        // rng seed, used for the random gens below
        mt19937 rngSeed;

        uniform_real_distribution<double> percentGen;
        uniform_int_distribution<int> itemsInOrder;
        uniform_int_distribution<int> itemChoice;
        uniform_int_distribution<int> ordersPerDay;

        normal_distribution<float> lunchOrderTimes;
        normal_distribution<float> dinnerOrderTimes;
        bernoulli_distribution orderChoice;

        void setupRandomGenerators();
        void generateItems();
        void generateLocations();
        void generateUsers();
        void generateIngredients(time_t startTime);
        void generateIngredientRelations();

        unique_ptr<OrderObject> generateOrderObject(int counts[]);
        void regenerateOrderObject(OrderObject* order, int count, ItemCategory category);
        unique_ptr<Order> generateOrder(time_t timeOfOrder);

    public:
        void generateNextDay();
        void generateNDays(int days);

        void writeTruncates(ofstream& stream) const;

        void writeCategories(ofstream& stream) const;
        void writeItems(ofstream& stream) const;
        void writeInventory(ofstream& stream) const;
        void writeInventoryRelations(ofstream& stream) const;
        void writeDays(ofstream& stream) const;
        void writeLocations(ofstream& stream) const;
        void writeUsers(ofstream& stream) const;
        void writeTimetables(ofstream& stream) const;
        void writeTeardown(const char* filename) const;
        void writeReset(const char* filename) const;

        void writeAll(const char* filename) const;

        void writeFast(ofstream& stream, Scriptable** arr, int arrLen) const;

        void writeTruncatesFast(ofstream& stream) const;
        void writeCategoriesFast(ofstream& stream) const;
        void writeItemsFast(ofstream& stream) const;
        void writeInventoryFast(ofstream& stream) const;
        void writeInventoryRelationsFast(ofstream& stream) const;
        void writeDaysFast(ofstream& stream) const;
        void writeLocationsFast(ofstream& stream) const;
        void writeUsersFast(ofstream& stream) const;
        void writeTimetablesFast(ofstream& stream) const;

        void writeAllFast(const char* filename) const;

        Generator(time_t startDay, const int peakDays) : current(0), peakDaysLen(peakDays), currentDay(startDay) {
            this->peakDays.resize(peakDays);

            generateItems();

            random_device rd{};
            rngSeed = std::mt19937(rd());
            setupRandomGenerators();  

            generateLocations();
            generateUsers();
            generateIngredients(time(nullptr));
            generateIngredientRelations();

            // Just select a random location for now.
            currentLocation = allLocations[(int)std::round(percentGen(rngSeed) * (allLocationsLen - 1))].get();

            // Just select a random user for now.
            currentUser = allUsers[(int)std::round(percentGen(rngSeed) * (allUsersLen - 1))].get();
        }
        Generator() : Generator(0, 0) {}

        ~Generator() {
        }
};
