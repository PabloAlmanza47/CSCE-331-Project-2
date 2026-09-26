CXX = g++
CXXFLAGS = -std=c++17 -Wall -fsanitize=undefined,address

all: generator

generator: generator.o
		${CXX} ${CXXFLAGS} generator.o -o generate
		./generate

generator.o: generator.cpp
		${CXX} ${CXXFLAGS} -c generator.cpp

.PHONY: clean
clean:
	rm -f *.o
	rm generate