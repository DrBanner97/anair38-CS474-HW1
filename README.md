# CS474 Homework 1 (Aditya Nair - anair38)

Implementation of my Domain Specific Language called myDSL using Scala for writing and evaluation Binary Operations on Set

## Features

- ### Operations on Set:

  1. Create
  2. Insert
  3. Delete

- ### Binary Set Operations:

  1. Union
  2. Intersection
  3. Set Difference
  4. Symmetric Difference
  5. Cartesian Product

- ### Macros
  Storing expressions as macros to have them expand and evaluate to expressions when macro-definition is used
- ### Scopes
  Storing and assigning variables with the additional ability to separate them into named and anonymous scopes

## Installation

The project works on **Scala 3.1.0** with sbt and Java **openjdk-17**.
Please ensure sbt is installed on the system before using the project.\
In IntelliJ also check

> File > Project Structure > Libraries

to ensure **Scala 3.1.0** is added as library.

## Instructions to run

> src/test/scala/myDSL/myDSL.scala

contains all of the code for this project

Navigate to the following path to get to the test file

> src/test/scala/myDSLTest.scala

Press the play button next to **myDSLTest** class to run all the test cases
