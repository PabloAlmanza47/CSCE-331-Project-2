#include <iostream>
#include <string>
#include <vector>
#include <ctime>
#include <random>

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

struct AmountPerCategory {
    int entrees, sides, drinks;

    AmountPerCategory() : entrees(0), sides(0), drinks(0) {}
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
        uint amount;
        Item* item;

    public:
        OrderObject(int orderObjId, int itemId, uint amount, Item* item) {
            id = orderObjId;
            parentId = -1;
            this->itemId = itemId;
            this->amount = amount;
            this->item = item;
        }
        OrderObject(int itemId, uint amount, Item* item) {
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
        uint& getAmount() noexcept;
};

class Order : public Scriptable {
    private:
        int id;
        vector<OrderObject*> items;
        time_t timestamp;

    public:
        Order(int id, const vector<OrderObject*>& items, const time_t& timestamp) {
            this->id = id;
            this->items = items;
            this->timestamp = timestamp;

            setChildIds();
        }
        Order(int id, vector<OrderObject*>&& items, const time_t& timestamp) {
            this->id = id;
            this->items = items;
            this->timestamp = timestamp;

            setChildIds();
        }
        Order(vector<OrderObject*>& items, const time_t& timestamp) {
            this->id = getId();
            this->items = items;
            this->timestamp = timestamp;

            setChildIds();
        }
        Order(vector<OrderObject*>&& items, const time_t& timestamp) {
            this->id = getId();
            this->items = items;
            this->timestamp = timestamp;

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
        vector<vector<Order*>*> orders;
        vector<Item*> allItems;
        time_t currentDay;
        mt19937 rngSeed;

        uniform_real_distribution<double> percentGen;
        uniform_int_distribution<int> itemsInOrder;
        uniform_int_distribution<int> itemChoice;
        uniform_int_distribution<int> ordersPerDay;

        void setupRandomGenerators();
        void generateItems();
        OrderObject* generateOrderObject(AmountPerCategory counts);
        Order* generateOrder(time_t durationOfOrder);

    public:
        void generateNextDay();
        void generateNDays(int days);
        void writeDays(const char* filename);
        void writeDays(ofstream& stream);
        void writeItems(const char* filename);
        void writeItems(ofstream& stream);

        void writeAll(const char* filename);

        Generator(time_t start_day) {
            currentDay = start_day;

            generateItems();

            random_device rd;
            rngSeed = std::mt19937(rd());
            setupRandomGenerators();
        }
        Generator() : Generator(0) {}

        ~Generator() {
            for (size_t i = 0; i < allItems.size(); ++i)
                delete allItems[i];

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