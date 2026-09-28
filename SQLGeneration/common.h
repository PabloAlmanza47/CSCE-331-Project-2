#include <iostream>
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

        virtual string toSql() const;

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

        string toSql() const override;

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
        string password;
        UserPerm perm;

    public:
        User(int userId, string password, UserPerm perm) : userId(userId), password(password), perm(perm) {}

        string toSql() const override;

        int getUserId() const noexcept;
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

        string toSql() const override;

        int getId() const noexcept;
        float getPrice() const noexcept;
        ItemCategory getCategory() const noexcept;
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

        string toSql() const override;
        void setParentId(int id);

        Item*& getItem() noexcept;
        int& getItemId() noexcept;
        int& getAmount() noexcept;
};

class Order : public Scriptable {
    private:
        int id;
        int userId;
        int locationId;
        vector<OrderObject*> items;
        time_t timestamp;

    public:
        Order(int id, int userId, int locationId, const vector<OrderObject*>& items, const time_t& timestamp) : id(id), userId(userId), locationId(locationId), items(items), timestamp(timestamp)  {
            setChildIds();
        }
        Order(int id, int userId, int locationId, vector<OrderObject*>&& items, const time_t& timestamp)  : id(id), userId(userId), locationId(locationId), items(items), timestamp(timestamp) {
            setChildIds();
        }
        Order(int userId, int locationId, vector<OrderObject*>& items, const time_t& timestamp)  : id(getId()), userId(userId), locationId(locationId), items(items), timestamp(timestamp) {
            setChildIds();
        }
        Order(int userId, int locationId, vector<OrderObject*>&& items, const time_t& timestamp) : id(getId()), userId(userId), locationId(locationId), items(items), timestamp(timestamp) {
            setChildIds();
        }

        string toSql() const override;
        void setChildIds();
        time_t getTimestamp() const noexcept;

        ~Order() override {
            for (size_t i = 0; i < items.size(); ++i)
                delete items[i];
        }
};

class Generator {
    private:
        // all item pointers, in an array. Sorted by category.
        Item** allItems;
        int allItemsLen;

        // How many items are in each category.
        int categoryLens[CATEGORY_LENGTH];

        // which days are peak days.
        int* peakDays;
        // when the next peak day is (if this equals peakDaysLen then there are no more peak days).
        int current;
        // length of the peakDays array.
        int peakDaysLen;

        // all used panda express locations 
        Location** allLocations;
        int allLocationsLen;

        // all employees
        User** allUsers;
        int allUsersLen;

        // Every order placed
        vector<vector<Order*>*> orders;

        // The current location and employee
        Location* currentLocation;
        User* currentUser;

        // The current day
        time_t currentDay;

        // rng seed, used for the random gens below
        mt19937 rngSeed;

        uniform_real_distribution<double> percentGen;
        uniform_int_distribution<int> itemsInOrder;
        uniform_int_distribution<int> itemChoice;
        uniform_int_distribution<int> ordersPerDay;

        void setupRandomGenerators();
        void generateItems();
        void generateLocations();
        void generateUsers();

        OrderObject* generateOrderObject(int counts[]);
        void regenerateOrderObject(OrderObject* order, int count, ItemCategory category);
        Order* generateOrder(time_t durationOfOrder);

    public:
        void generateNextDay();
        void generateNDays(int days);

        void writeEnumTypes(ofstream& stream) const;
        void writeTruncates(ofstream& stream) const;

        void writeCategories(ofstream& stream) const;
        void writeItems(ofstream& stream) const;
        void writeDays(ofstream& stream) const;

        void writeAll(const char* filename) const;

        Generator(time_t startDay, const int peakDays) : current(0), peakDaysLen(peakDays), currentDay(startDay) {
            this->peakDays = new int[peakDays];

            generateItems();
            generateLocations();
            generateUsers();

            random_device rd;
            rngSeed = std::mt19937(rd());
            setupRandomGenerators();  
            
            // Just select a random location for now.
            currentLocation = allLocations[(int)std::round(percentGen(rngSeed) * (allLocationsLen - 1))];

            // Just select a random user for now.
            currentUser = allUsers[(int)std::round(percentGen(rngSeed) * (allUsersLen - 1))];
        }
        Generator() : Generator(0, 0) {}

        ~Generator() {
            delete[] peakDays;
            
            for (int i = 0; i < allLocationsLen; ++i)
                delete allLocations[i];
            delete[] allLocations;

            for (int i = 0; i < allUsersLen; ++i)
                delete allUsers[i];
            delete[] allUsers;

            for (int i = 0; i < allItemsLen; ++i)
                delete allItems[i];
            delete[] allItems;

            for (size_t i = 0; i < orders.size(); ++i) {
                vector<Order*>* current = orders[i];
                for (size_t j = 0; j < current->size(); ++j) {
                    Order* o = (*current)[j];
                    delete o;
                }
                delete current;
            }
        }
};