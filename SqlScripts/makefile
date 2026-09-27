CXX = g++
CXXFLAGS = -std=c++17 -Wall -Wextra -pedantic-errors -fsanitize=undefined,address -g

all: generator

generator: generator.o
		${CXX} ${CXXFLAGS} generator.o -o generate

generator.o: generator.cpp
		${CXX} ${CXXFLAGS} -c generator.cpp

run: generator
		./generate

.PHONY: clean
clean:
	rm -f *.o
	rm -f generate
	rm -f ../SqlCommands/Script.sql