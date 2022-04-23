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
            return 42;
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
        DeclareInstance(class_defintion: ClassDef("someClassName"), instance_name: "inst1").eval()
      ```
    - #### NewObject
      instantiates a class definition. invokes constructor
      ```.scala
        NewObject(class_definition: ClassDef("someClassName"))
      ```   
    - #### AssignInstance
      binds instance name to instance
      ```.scala
        AssignInstance(class_instance: NewObject(ClassDef("someClassName")), instance_name: "inst1").eval()
      ``` 
    - #### Class Instance operation
        - GetField
          fetching instance variables of an instance
          ```.scala
            Instance(instance_name: "inst1", GetField(variable_name:"x")) 
            // java counterpart: inst1.x
          ```
        - InvokeMethod
          ```.scala
            Instance(instance_name: "inst1", InvokeMethod(method_name:"m1", parameters: List(10), ) 
            // java counterpart: inst1.m2(10)
          ```
  - ### Class Inheritance
    Inheritance in Java
    ```java
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
    ```scala
    ClassDef("subClass", 
             Field("a", "public",Value(3)),
             Constructor(Assign("a",Value(4))), 
             Method("f1", "public",List(),Var("a"))
            ) Extends ClassDef("someClassName")
    ```

## HW3 Features
- ### Interface Definition  
  Assume a simple interface in Java
  ```java
  // defining an interface in java
  
  interface someInterface {
    public int a = 10
    public abstract method1();
  }
  
    ```
    the above interface definition in myDSL would look like:
    ```scala
    InterfaceDef("someInterface",
      Field("a", AccessType.PUBLIC, Value(10)),
      Method("method1", AccessType.PUBLIC, ImplementationType.ABSTRACT, List(), Value(1))
    ).eval()
    ```
  

- InterfaceDef only accepts ```abstract``` implementation of methods
- Does not accept ```private``` fields
- Returns the definition of interface when no arguments are provided

- ### Interface Implementation
  assume a class that implements the above created interface
    ```java
    public class someClassName implements someInterface{
        public int x = 47;
    
        //constructor
        void someClassName(){
            x = 2;
        }
  
        @Override
        public void method1(){
            int z = 33;
        }     
    }  
    ```
  in myDSL that would look like
    ```scala
    ClassDef("someClassName1",
        Field("x", AccessType.PUBLIC, Value(47)),
        Constructor(Assign("x", Value(2))),
        Method(
          "method1",
          AccessType.PUBLIC,
          ImplementationType.CONCRETE,
          List(),
          DeclareVar("z",Value(33))
        )
      ).eval()
  
    ClassDef("someClassName1") Implements List(InterfaceDef("someInterface"))

    ```
- where ```Implements``` accepts a ```List``` of ```InterfaceDef```, that a class can implement

  - ###Abstract Class
      assume an abstract class in java 
      ```java
        abstract class AbsClass1{
          public int y = 10
          abstract someMethod1();
        }
      ```
    in myDSL would be declared in the following way
      ```scala
        AbstractClassDef("AbsClass1",
          Field("y", AccessType.PUBLIC, Value(10)),
          Method("someMeth1",
            AccessType.PUBLIC,
            ImplementationType.ABSTRACT,
            List()
          )
        ).eval()
      ```
      ```AbstractClassDef``` requries atleast one abstract method as an argument and cannot be instantiated
    
      Inheritance with abstract class work just like inheritance with classes
      ```scala
      ClassDef("someClassName1") Extends AbstractClassDef("AbsClass1")
    ```

## Limitations

myDSL uses `Any` as a data type to deal with varying data inputs (String, Int....). This might cause problems by undermining Scala's strongly typed system, as evident with usage of `asInstanceOf` methods and type matching to ensure the input is of a certain data type.

### Questions to address

- **Can a class/interface inherit from itself?** 
  - No, a class/interface cannot inherit from itself. The Extends function checks for cyclic dependecy among two (static and dynamic) class/interface, by traversing a Map that defines inheritance between classes/interfaces. The function checks this cyclic dependency for both the class/interface involved, thereby taking care of a situation where a class/interface is made to inherit from itself

- **Can an interface inherit from an abstract class with all pure methods?** 
  - No in myDSL, interface can only inherit from other interfaces.

- **Can an interface implement another interface?** 
  - No, interfaces cannot implement other interface. myDSL dissallows Interface definitions to invoke the Implements method.

- **Can a class implement two or more different interfaces that declare methods with exactly the same signatures?** 
  - Yes, classes will be able to implement interfaces with that have methods with same signatures. myDSL checks for presence of method names in concrete class implementations, to ensure that abstract methods are overriden. Therefore existance of one concrete implementation of said method name should qualify and allow implementation of all those interfaces.

- **Can an abstract class inherit from another abstract class and implement interfaces where all interfaces and the abstract class have methods with the same signatures?** 
  - Yes, for rationale mentioned above abstract classes will be allowed to do so.

- **Can an abstract class implement interfaces?** 
  - Yes, abstract class can implement interfaces. Abstract class definition needs to be specifically defined, once that is done, the abstract class name and definitions are stored and treated similar to Class Definitions hence allowing Implements functionality

- **Can a class implement two or more interfaces that have methods whose 
signatures differ only in return types?** 
  - Since myDSL doesn't concretely define return types for methods and returns the last statement of the method, classes will be able to implements multiple interfaces with same methods but different return types.

- **Can an abstract class inherit from a concrete class?** 
  - Yes, abstract class much like a concrete class definition would be able to inherit from a concrete class provided they do not have circulaar dependency.

- **Can an abstract class/interface be instantiated as anonymous concrete classes?** 
  - Instantiation has been limited to concrete class definition, therefore you cannot instantiate abstract class/interface as anonymous concrete classes.


## HW4 Features
- ### Conditions
    assume an if-else structure in java
    ```java
  int x = 7;
    if(x == 7)
        x = 10;
    else
        x = 20;
  ```
  in myDSL that would look like
  ```scala
  If(Check(Var("x"), 7), //condition 
      AnonScope(Assign("x", 10)), // statements to execute when condition is true
      AnonScope(Assign("x", 20))) // statements to execute when condition is false

  ```
  code-blocks to execute when true/false are wrapped in AnonScope (Anonymous Scope)

- ### Exception handling
    Exception Handling in java would look like
    ```java
  
    try{
        int x = 7;
        throw NumberFormatException();
        x = 10;
    }
    catch(NumberFormatException e){
        e.message;
    }  
   ```
  in myDSL would look like:
    ```scala
    CatchException("NumberFormatException", //name of exception to handle
        Catch("exceptionVariable", Instance("exceptionVariable", GetField("message"))), //statements to execute when exception is thrown 
        DeclareVar("x", Value(7)), //
        ThrowException(ClassDef("NumberFormatException"), AssignField("message", "forced error thrown")),
        Assign("x", Value(10))
      )
  ```
  
    where ```NumberFormatException```  would have to be declared explicitly in the following way:
    ```scala
    ExceptionClassDef("someExceptionName", Field("message", AccessType.PUBLIC)).eval()
   ```
  
## HW5 Features
- ### Partial Evalutation
    myDSL has been modified to allow Partial Evaluation. myDSL not returns ```Either[Any,Any]```. 
    ```Left[+A]``` returned denotes partially evaluated expression \
    ```Right[+A]``` returned denotes partially evaluated expression
    
    Assume an If construct
    ```scala
    If(
        Check(Var("a"), Var("b")),
        AnonScope(Var("x"), Var("y"), Value(3)),
        AnonScope(Insert(Var("a"), Value(4))),
      )
    
    ```
    
    when evaluated in myDSL would return 
    ```scala
      Left(
        If(
          Check(Var(a),Var(b)),
          AnonScope(ArraySeq(Var(x), Var(y), Value(3))),
          AnonScope(ArraySeq(Insert(Var(a),ArraySeq(Value(4)))))
        )
      )
    ```
    
    ```Left``` denoting a partially evaluated If condition with 
    
    Assume values for ```Var("a")``` and ``` Var("y")```are provided
    ```scala
      DeclareVar("a", CreateSet(Value(1), Value(2), Value(3))).eval()
      DeclareVar("y", Value(2)).eval()
    ```
  
    ```scala
      Left(
            If(Check(Value(Set(1, 2, 3)),Var("b")),
              AnonScope(ArraySeq(Var("x"), Value(2), Value(3))),
              AnonScope(ArraySeq(Value(Set(1, 2, 3, 4)))))
          )
    ```
- ### Monads
    Monads can be used by invoking ```myDSLExpMonad```and passing an ```Exp``` \
    Further the ```map``` function can be used to perform operations on the ```Exp``` passed

    Take the following applicaation as an example:
    ```scala
    def CartProductComputation(exp: Exp):Exp = {
        exp.eval() match{

          case Right(immutableSet: Set[Any])=>
            Value(Product(Value(immutableSet), Value(immutableSet)).eval().merge)
          case Left(value: Exp)=>
            Product(value,value)
        }
      }

      myDSLExpMonad(CreateSet(Value(1), Value(2), Value(3)))
        .map(CartProductComputation) // Value(Set((2,2), (2,1), (1,2), (1,1), (3,2), (3,1), (3,3), (2,3), (1,3))
    
    ```
  For examples with chaining monads check  ```myDSLPartialEvaluation.scala```




