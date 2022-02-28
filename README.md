# CS474 Homework  (Aditya Nair - anair38)

Implementation of my Domain Specific Language called myDSL using Scala for writing and evaluation Binary Operations on Set

## Installation

The project works on **Scala 3.1.0** with [sbt](https://www.scala-sbt.org/) and Java [**openjdk-17**](https://openjdk.java.net/).

- Please ensure sbt is installed on the system before using the project.
- Install [IntelliJ IDEA](https://www.jetbrains.com/idea/)
- In IntelliJ ensure the [scala plugin](https://plugins.jetbrains.com/plugin/1347-scala) is installed.
- Once installed, in IntelliJ go to:

  > File > Project Structure > Libraries

  and ensure **Scala 3.1.0** is added as library.

- Download this GitHub repository and open that folder using IntelliJ IDEA

## Instructions to run

> src/main/scala/myDSL/myDSL.scala

contains all code for this project

Navigate to the following path to get to the test file

> src/test/scala/myDSLTest.scala

Press the play button next to **myDSLTest** class to run all the test cases

## HW1 Features

- ### Operations on Variables

  1. Declare variable
     ```.scala
     DeclareVar("a", Value(1)).eval()
     ```
  2. Referring to variable

     ```.scala
     Var("a").eval() // outputs 1
     ```

  3. Re-assign variable

     ```.scala
     Assign("a", Value(2)).eval()
     ```

- ### Operations on Set:

  1. Create Set

     ```.scala
     DeclareVar("b", CreateSet(Value(1), Value(2), Value(3)).eval() //declares variable "b" with immutable Set(1,2,3)
     ```

  2. Insert

     ```.scala
     Assign("a",Insert(Var("a"), Value(4))).eval() //alters variable a to insert 4
     Insert(CreateSet(Value(3), Value(4)), Value(5), Value(6)).eval() // also works with Sets that haven't been assigned to any variables
     ```

  3. Delete

     ```.scala
     Delete(Var("a"), Value(4)).eval()
     Delete(Var("a"), Value(4), Value(1)).eval()
     ```

     NOTE: **CreateSet**, **Insert** and **Delete** take the Set as first parameter and accepts any number of valid **Value** to Create/Insert/Delete from set

- ### Binary Set Operations:

  1. Union

     ```.scala
     //perform Union on undeclared variables
     Union(CreateSet(Value(1), Value(2)), CreateSet(Value(2), Value(3)).eval()

     //perform Union on existing variables
     Union(Var("a"), Var("b")).eval()
     ```

  2. Intersection

     ```.scala
     //perform Intersection on undeclared variables
     Intersection(CreateSet(Value(1), Value(2)), CreateSet(Value(2), Value(3)).eval()

     //perform Intersection on existing variables
     Intersection(Var("a"), Var("b")).eval()
     ```

  3. Set Difference

     ```.scala
     //perform Set Difference on undeclared variables
     Diff(CreateSet(Value(1), Value(2)), CreateSet(Value(2), Value(3)).eval()

     //perform Set Difference on existing variables
     Diff(Var("a"), Var("b")).eval() //set diff
     ```

  4. Symmetric Difference

     ```.scala
     //perform Symmetric Difference on undeclared variables
     SymmetricDifference(CreateSet(Value(1), Value(2)), CreateSet(Value(2), Value(3)).eval()

     //perform Symmetric Difference on existing variables
     SymmetricDifference(Var("a"), Var("b")).eval() //set diff
     ```

  5. Cartesian Product

     ```.scala
     //perform Cartesian Product on undeclared variables
     Product(CreateSet(Value(1), Value(2)), CreateSet(Value(2), Value(3)).eval()

     //perform Cartesian Product on existing variables
     Product(Var("a"), Var("b")).eval() //set diff
     ```

  NOTE: None of the Set Operators or Binary Operators alter the variable entries themselves. They are referentially transparent. For them to take effect use **DeclareVar** or **Assign** as per use case

- ### Macros

  ```.scala
  // consider Set a and b
  DeclareVar("a", CreateSet(Value(1),Value(2),Value(3))).eval()
  DeclareVar("b", CreateSet(Value(3), Value(4), Value(5))).eval()

  //define a Macro
  SetMacro("combine_sets", Union(Var("a"), Var("b"))).eval()

  DeclareVar("union_a_b", GetMacro("combine_sets")).eval() //declares a variable union_a_b with immutable Set(1,2,3,4,5) resulting from Union of Set "a" and "b"
  ```

- ### Scopes

  ```.scala
    //declaring variable c in scope s1
    Scope("s1", DeclareVar("c", CreateSet(Value(44), Value(45), Value(46)))).eval()

    //re-assigning variable a from Anonymous Scope
    AnonScope(Assign("a", Value(42))).eval()
  ```
## HW2 Features
  - ### Class Definition
      Assume a simple class in Java
      ```.java
     // defining a class in java
    public class someClassName{
        private int f = 1;
        public int x = 4
    
        //constructor
        void someClassName(){
            f = 2;
        }
        private int m1(int z){
            return z;
        }
        public int m2(){
            retunr 42;
         } 
    }  
    ```
    the above class definition in myDSL would look like:
    ```.scala
 
    ClassDef("someClassName", 
             Field("f", "private",Value(1)),
             Field("x", "public", Value(4)), 
             Constructor(Assign("f", Value(2))), 
             Method("m1", "private",List("z"),Var("z")), 
             Method("m2", "public", List(), Value(42))
             ).eval()
    ```
    
    where 
    - #### Field
      Field defines instance variables in a class
      ```.scala
        Field(name: "f", access modifier: "public"/"private", value: Value(2))
      ```
    - #### Constructor
      Constructor  function for a class and is invoked when instantiated
      ```.scala
        Constructor(instructions: Assign("f", Value(2))) //accepts ArraySeq of instructions
      ```
    - #### Method
        Method function for a class   
      ```.scala
        Method(methodName: "m1", access modifier: "private", parameterNames: List("z"),instructions: Var("z")),
      ```  
  - ### Class Instantiation
    A simple class instantiation in java
       ```.java
     // defining a class in java
    SomeClassName inst1;
    inst1 = new SomeClass()
    ```
    class instantiation in myDSL
    ```.scala
      DeclareInstance(ClassDef("someClassName"),"inst1").eval()
      AssignInstance(NewObject(ClassDef("someClassName")), "inst1").eval()
    ```
    where
    - #### DeclareInstance
      declares a variable instance of type a class type
      ```.scala
        DeclareInstance(class defintion: ClassDef("someClassName"), instance name: "inst1").eval()
      ```
    - #### NewObject
      instantiates a class definition. invokes constructor
      ```.scala
        NewObject(class definition: ClassDef("someClassName"))
      ```   
    - #### AssignInstance
      binds instance name to instance
      ```.scala
        AssignInstance(class instance: NewObject(ClassDef("someClassName")), instance name: "inst1").eval()
      ``` 
    - #### Class Instance operation
        - GetField
          fetching instance variables of an instance
          ```.scala
            Instance(instance name: "inst1", GetField(variable name:"x")) 
            // java counterpart: inst1.x
          ```
        - InvokeMethod
          ```.scala
            Instance(instance name: "inst1", InvokeMethod(method name:"m1", parameters: List(10), ) 
            // java counterpart: inst1.m2(10)
          ```
  - ### Class Inheritance
    Inheritance in Java
    ```.java
    public class subClass extends someClassName {
      public int a = 3;
      void subClass (){
        a = 4
      }
    
      public int f1(){
       return a;    
      }
    ```
    class inheritance in myDSL would look like
    ```.scala
    ClassDef("subClass", 
             Field("a", "public",Value(3)),
             Constructor(Assign("a",Value(4))), 
             Method("f1", "public",List(),Var("a"))
            ) Extends ClassDef("someClassName")
    ```
  
    



## Limitations

myDSL uses `Any` as a data type to deal with varying data inputs (String, Int....). This might cause problems by undermining Scala's strongly typed system, as evident with usage of `asInstanceOf` methods and type matching to ensure the input is of a certain data type.
