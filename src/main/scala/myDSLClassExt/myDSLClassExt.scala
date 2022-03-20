package myDSLClassExt
import scala.language.implicitConversions
import myDSL.myDSL.Exp
import myDSL.myDSL.Exp.*
import common.Common.*
import jdk.jshell.spi.ExecutionControl.NotImplementedException

import java.util.NoSuchElementException
import scala.collection.immutable.ArraySeq
import scala.collection.mutable

object myDSLClassExt:


  type BasicType = Any
  type mutableMapAny = scala.collection.mutable.Map[String, BasicType]
  type mutableMapString = scala.collection.mutable.Map[String, String]

  private val classInheritance: mutable.Map[String, String] = mutable.Map() // used to keep track of class inheritance chain
  private val interfaceImpl: mutable.Map[String, List[String]] = mutable.Map() // used to keep track of class inheritance chain
  classInheritance += FIELD_TYPE_VARIABLE -> null
  private val outerScope: mutableMapAny = mutable.Map()
  outerScope +=  FIELDS -> mutable.Map[String, BasicType]() //kinda redundant
  // Field types used to differentiate between vars and class object instances
  outerScope +=  FIELD_TYPES -> mutable.Map[String, BasicType]() //kinda redundant
  outerScope +=  METHODS -> mutable.Map[String, String]() //kinda redundant
  outerScope +=  CLASS_DEFINITION -> mutable.Map[String, String]()
  outerScope +=  INTERFACE_DEFINITION -> mutable.Map[String, BasicType]()
//  outerScope +=  ABSTRACT_CLASS_DEFINITION -> mutable.Map[String, BasicType]()


  enum AccessType:
    case PRIVATE
    case PUBLIC
    case PROTECTED

  enum ImplementationType:
    case CONCRETE
    case ABSTRACT

  enum EntityType:
    case CLASS
    case INTERFACE
    case ABSTRACT_CLASS


  //noinspection ScalaDocUnknownParameter
  enum myDSLClassExt:
    case ClassDef(name: String, args: myDSLClassExt*) //to define and reference class definition
    case Field(varName: String, accessType: AccessType  ,value: BasicType*) //to define Fields within ClassDef
    case GetField(fieldName: String)//to fetch fields from an instance of a class
    case Constructor(params:Exp*) //to define constructor within ClassDef
    case Method(methodName: String,
                accessType: AccessType,
                implementationType: ImplementationType,
                parameters: List[BasicType],
                instructions:BasicType*
               ) //to define method within ClassDef
    case Instance(instanceName: String, operation: myDSLClassExt) //used to reference an instance of class and perform instance based operation (get fields, methods and inner classes)
    case DeclareInstance(classDef: myDSLClassExt, varName: String) // used to declare an instance
    case AssignInstance(classDef: myDSLClassExt, varName: String) //uses previous declaration to assign a new instance
    case NewObject(classDef: ClassDef) // creates an instance of a ClassDef, invoked class constructor
    case InvokeMethod( methodName: String, parameters: List[BasicType]) //invokes a method in a given instance
    case InterfaceDef(name: String, args: myDSLClassExt* )
    case Interface(name: String, operation: myDSLClassExt)
    case AbstractClassDef(name: String, args: myDSLClassExt*)

    /**
     *  SomeClass s =  new SomeSubClass
     * @param lhsClass: denotes SomeClass
     * @param rhsClass: denotes SomeSubClass
     * */
    def substitution(lhsClass: String, rhsClass: String): Boolean = {

      if(lhsClass == rhsClass)
        true
      else if(classInheritance(rhsClass) == null)
        false
      else
        substitution(lhsClass,classInheritance(rhsClass))
    }

    def interfaceSubstitution(lhsInterface: String, rhsClass: String): Boolean = {

      if(lhsInterface == null || rhsClass == null)
        false
      else if(interfaceImpl(lhsInterface).contains(rhsClass))
        true
      else
        interfaceSubstitution(classInheritance(lhsInterface), rhsClass)||
          interfaceSubstitution(lhsInterface, classInheritance(rhsClass))

    }





    def defineEntity (input: myDSLClassExt, name: String, args: Seq[myDSLClassExt]): mutableMapAny =

      val currentScope: mutableMapAny= scala.collection.mutable.Map()
      currentScope +=  FIELDS -> mutable.Map[String, BasicType]()
      currentScope +=  FIELD_TYPES -> mutable.Map[String, BasicType]()
      currentScope +=  FIELD_ACCESS_TYPES -> mutable.Map[String, AccessType]()
      currentScope +=  METHODS -> mutable.Map[String, BasicType]()
      currentScope +=  METHOD_ACCESS_TYPES -> mutable.Map[String, AccessType]()
      currentScope +=  METHOD_IMPLEMENTATION_TYPES -> mutable.Map[String, ImplementationType]()
      currentScope +=  CLASS_DEFINITION -> mutable.Map[String, BasicType]()
      currentScope +=  INTERFACE_DEFINITION -> mutable.Map[String, BasicType]()
//      currentScope +=  ABSTRACT_CLASS_DEFINITION -> mutable.Map[String, BasicType]()
      currentScope +=   CLASS_NAME -> name

      for( arg <- args){
        arg match {

          /**
           * @param params*: ArraySeq of Exp (Set theory) operations to execute in order
           * invoked when NewObject is called
           * returns evaluation of last statement
           */
          case Constructor(params*) =>

            //only allowing constructors for Class Definitions
            this match {

              case ClassDef(name: String, args*) =>
                val constructorScope: mutableMapAny = scala.collection.mutable.Map()
                constructorScope += "parent" -> currentScope
                constructorScope += FIELDS -> scala.collection.mutable.Map[String, String]()
                constructorScope(FIELDS).asInstanceOf[mutableMapAny] += "parent" -> currentScope(FIELDS)
                constructorScope += FIELD_TYPES -> scala.collection.mutable.Map[String, BasicType]()
                constructorScope += METHOD_INSTRUCTIONS -> params
                constructorScope += METHOD_IMPLEMENTATION_TYPES -> ImplementationType.CONCRETE // Always concrete


                currentScope(METHODS).asInstanceOf[mutableMapAny] += name -> constructorScope

              case _: Any => throw Exception("Cannot have constructor for " + this)
            }

          /**
           * @param methodName: name of the method to define
           * @param accessType: access modifiers, accepts string values "public" and "private"
           * @param params*: ArraySeq of Exp (Set theory) operations to execute in order
           * invoked using InvokeMethod
           * returns evaluation of last statement
           */
          case Method(methodName, accessType, modifier, parameters, instructions*) =>
//            print("implementation type: "+modifier+" instruction len: "+instructions.length)
            if (methodName == name) throw Exception("method name cannot be the same as class name")
            else if(modifier == ImplementationType.ABSTRACT && instructions.length>0)
              throw Exception(""+this+" abstract method "+methodName+" cannot have body")
            else
              val methodScope: mutableMapAny= scala.collection.mutable.Map()

              methodScope += "parent" -> currentScope
              methodScope += FIELDS -> (scala.collection.mutable.Map[String, BasicType]() ++ parameters.map(paramName=> paramName.asInstanceOf[String] ->0))
              methodScope(FIELDS).asInstanceOf[mutableMapAny] += "parent"-> currentScope(FIELDS)
              methodScope += METHOD_INSTRUCTIONS -> instructions
              currentScope(METHODS).asInstanceOf[mutableMapAny] += methodName -> methodScope
              currentScope(METHOD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, AccessType]] += methodName -> accessType


              currentScope(METHOD_IMPLEMENTATION_TYPES).asInstanceOf[scala.collection.mutable.Map[String, ImplementationType]] += methodName -> modifier


          /**
           * @param varName: name of the field
           * @param accessType: access modifiers, accepts string values "public" and "private"
           * @param value*: optional value field
           *
           * defines instance variable in a class scope
           */
          case Field(varName, accessType ,value*) =>
            val classScopeFields = currentScope(FIELDS).asInstanceOf[mutableMapAny]
            if (value.length == 0)
              Exp.DeclareVar(varName = varName, value = Value(0)).eval(classScopeFields)
            else
              value(0) match {
                case exp: Exp => Exp.DeclareVar(varName = varName, value = exp).eval(classScopeFields)
                case _: Any => throw Exception("Invalid field values")
              }
            currentScope(FIELD_TYPES).asInstanceOf[mutableMapAny] += varName -> FIELD_TYPE_VARIABLE
            currentScope(FIELD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, AccessType]] += varName -> accessType

          /**
           *  @param name: name of class
           *  @param args: class definitions like Constructor, Methods, Field, ClassDef(inner classes) go here
           *  used for class definition or fetching class definition (when args is not provided)
           */
          case ClassDef (name: String, args*) =>
            currentScope(CLASS_DEFINITION).asInstanceOf[mutableMapAny] += name -> ClassDef(name, args*).eval(currentScope)
          case InterfaceDef (name: String, args*) =>
            currentScope(INTERFACE_DEFINITION).asInstanceOf[mutableMapAny] += name -> InterfaceDef(name, args*).eval(currentScope)
          case AbstractClassDef (name: String, args*) =>
            currentScope(CLASS_DEFINITION).asInstanceOf[mutableMapAny] += name -> AbstractClassDef(name, args*).eval(currentScope)

          case _: Any => throw Exception("Invalid item found")
        }

      }

//        scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String,BasicType]] += name -> currentScope
//        classInheritance += name -> null
//      }


      currentScope


    def checkInterfaceForField(scope: mutableMapAny, valueToReturn: Exception, fieldName: String): BasicType =
      val classImplementedInterfaces = scope(IMPLEMENTED_INTERFACES).asInstanceOf[List[BasicType]]
      if(classImplementedInterfaces.isEmpty)
        valueToReturn
      else
        val returnedValues = mutable.Map[String, BasicType]()
        returnedValues += "__SOLUTION__" -> null
        //                      val interfaceWithValidValue: String = null
        classImplementedInterfaces.foreach(el =>
          val interfaceDef = el.asInstanceOf[mutableMapAny]
          val interfaceName = interfaceDef(CLASS_NAME).asInstanceOf[String]
          returnedValues += interfaceName -> GetField(fieldName).eval(interfaceDef, SCOPE_CALL_TYPE_OUTER)
          if(!returnedValues(interfaceName).isInstanceOf[Exception])
            returnedValues += "__SOLUTION__" -> interfaceName
        )
        if(returnedValues("__SOLUTION__") == null)
          valueToReturn
        else
          returnedValues(returnedValues("__SOLUTION__").asInstanceOf[String])



    def eval(scope: mutableMapAny = outerScope, scopeCall: String = SCOPE_CALL_TYPE_INNER): BasicType =


      this match {
        /**
         * @param name: variable name of the field
         * fetches field from given scope. with inherited classes GetField looks for field in parent class if not present
         */
        case GetField(name: String) =>

          val scopeFields = scope(FIELDS).asInstanceOf[mutableMapAny]
          scopeFields.get(name) match {
            case Some(value: BasicType) =>
              if(scopeCall == SCOPE_CALL_TYPE_OUTER && scope(FIELD_ACCESS_TYPES).asInstanceOf[ mutable.Map[String, AccessType]](name) == AccessType.PRIVATE)
                throw Exception("variable "+ name+" is private")
              else
                value
            case None =>
            scope(ENTITY_TYPE).asInstanceOf[EntityType] match{
                case EntityType.CLASS =>
                  scopeFields.get(PARENT_INSTANCE) match {
                  case Some(parentInstance: mutableMapAny ) =>
                    val valueToReturn = GetField(name).eval(parentInstance, SCOPE_CALL_TYPE_OUTER)

                    /**
                     * checking for fields in interfaces current class may have implemented
                     */
                    if(valueToReturn.isInstanceOf[Exception])
                      checkInterfaceForField(scope, valueToReturn.asInstanceOf[Exception], name)
                    else
                      valueToReturn
                  case None =>
                    checkInterfaceForField(scope, Exception("Field "+name+" not found"), name)
                }
                case EntityType.INTERFACE | EntityType.ABSTRACT_CLASS =>
                  scope.get("parent") match {
                    case Some(parent: mutableMapAny) =>
                      val valueToReturn = GetField(name).eval(parent, SCOPE_CALL_TYPE_OUTER)
                      if(valueToReturn.isInstanceOf[Exception])
                        throw valueToReturn.asInstanceOf[Exception]
                      else
                        valueToReturn
                    case None => Exception("Field "+name+" not found")
                  }
              }



          }

        /**
         * @param instanceName: variable name of the instance
         * evaluates operation in the scope of instance
         */
        case Instance(instanceName, operation)=>

          val instance=GetField(instanceName).eval(scope).asInstanceOf[mutableMapAny]

          //          val instance = scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, String]](instanceName)
          operation.eval(instance)

        /**
         * for evaluating operations specific to the scope of an interface
          */
        case Interface(name: String, operation) =>

          scope(INTERFACE_DEFINITION).asInstanceOf[mutableMapAny].get(name) match{

            case Some(interfaceDef: mutableMapAny) =>
              operation.eval(interfaceDef)
            case None => throw NoSuchElementException()

          }


        /**
         *  @param name: name of class
         *  @param args: class definitions like Constructor, Methods, Field, ClassDef(inner classes) go here
         *  used for class definition or fetching class definition (when args is not provided)
         */
        case ClassDef(name: String, args* )=>

          if(name == FIELD_TYPE_VARIABLE)
            throw Exception("class name cannot be "+FIELD_TYPE_VARIABLE)

          if(args.length >0) {
            val currentScope: mutableMapAny = defineEntity(this, name, args)

            currentScope += ENTITY_TYPE  -> EntityType.CLASS
            currentScope += IMPLEMENTED_INTERFACES  -> List[BasicType]()
            currentScope(METHODS).asInstanceOf[mutableMapAny].foreach((methodName, method)=>


            currentScope(METHOD_IMPLEMENTATION_TYPES).asInstanceOf[scala.collection.mutable.Map[String, ImplementationType]].
              foreach((k,v) => if(v == ImplementationType.ABSTRACT) throw Exception("Method "+ k+" in class " + name+" cannot be abstract" ))
            )

            scope(CLASS_DEFINITION).asInstanceOf[mutableMapAny] += name -> currentScope
            classInheritance += name -> null
          }
          //          val classDefs = scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, String]]


          scope(CLASS_DEFINITION).asInstanceOf[mutableMapAny](name)


        /**
         *  @param name: name of interface
         *  @param args: interface definitions like Methods, Field, ClassDef(inner classes) go here
         *  used for interface definition or fetching interface definition (when args is not provided)
         */
        case InterfaceDef(name: String, args* )=>

          if(name == FIELD_TYPE_VARIABLE)
            throw Exception("interface name cannot be "+FIELD_TYPE_VARIABLE)

          if(args.length >0) {
            val currentScope: mutableMapAny = defineEntity(this, name, args)
            currentScope += ENTITY_TYPE  -> EntityType.INTERFACE

            currentScope(METHOD_IMPLEMENTATION_TYPES).asInstanceOf[mutable.Map[String, ImplementationType]].
              foreach((k,v) => if(v == ImplementationType.CONCRETE) throw Exception("Method "+ k+" in interface " + name+" cannot have a body" ))

            currentScope(FIELDS).asInstanceOf[mutableMapAny].foreach((varName,value)=> if(value == null) throw Exception("value expected for variable "+varName+" in interface "+name))


            currentScope(FIELD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, AccessType]].foreach((k,v)=> if(v != AccessType.PUBLIC) throw Exception("modifier "+v+" not allowed for Interface"))


            scope(INTERFACE_DEFINITION).asInstanceOf[mutableMapAny] += name -> currentScope
            classInheritance += name -> null
            interfaceImpl += name -> List()
          }

          scope(INTERFACE_DEFINITION).asInstanceOf[mutableMapAny](name)


        /**
         *  @param name: name of abstract class
         *  @param args: abstract class definitions like Methods, Field, ClassDef(inner classes) go here
         *  used for abstract definition or fetching abstract class definition (when args is not provided)
         */
        case AbstractClassDef(name: String, args* )=>

          if(name == FIELD_TYPE_VARIABLE)
            throw Exception("abstract class name cannot be "+FIELD_TYPE_VARIABLE)

          if(args.length >0) {
            val currentScope: mutableMapAny = defineEntity(this, name, args)
            currentScope += ENTITY_TYPE  -> EntityType.ABSTRACT_CLASS


            if(!currentScope(METHOD_IMPLEMENTATION_TYPES).asInstanceOf[mutable.Map[String, ImplementationType]].exists(_._2 == ImplementationType.ABSTRACT))
              throw Exception("Abstract class "+name+" must have atleast one abstract method")

            scope(CLASS_DEFINITION).asInstanceOf[mutableMapAny] += name -> currentScope
            classInheritance += name -> null
          }


          scope(CLASS_DEFINITION).asInstanceOf[mutableMapAny](name)
        case DeclareInstance(classDef, varName) =>

//          val classDefs = scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, String]]

          val classScope = classDef.eval(scope).asInstanceOf[mutableMapAny]
          scope(FIELD_TYPES).asInstanceOf[mutableMapAny] += varName -> classScope
          scope(FIELDS).asInstanceOf[mutableMapAny] += varName -> null
          classScope

        /**
         * @param classDef: ClassDef type of the instance
         * @param varName: name of existing instance variable
         * binds instance of ClassDef to varName
         */
        case AssignInstance(classDef, varName) =>


          classDef.eval(scope) match{
            case classMap: mutableMapAny =>
              val instanceType  = scope(FIELD_TYPES).asInstanceOf[mutableMapAny]
              val currentScopeFields =scope(FIELDS).asInstanceOf[mutableMapAny]


              instanceType(varName) match {

                case entityDef: mutableMapAny =>

                  entityDef(ENTITY_TYPE).asInstanceOf[EntityType] match{

                    case EntityType.CLASS | EntityType.ABSTRACT_CLASS  =>
                      if (substitution(entityDef(CLASS_NAME).asInstanceOf[String], classMap(CLASS_NAME).asInstanceOf[String]))
                        classMap(FIELDS).asInstanceOf[mutableMapAny] ++= entityDef(FIELDS).asInstanceOf[mutableMapAny]
                        currentScopeFields += varName -> classMap
                        currentScopeFields(varName)
                      else
                        throw Exception("Class "+classMap(CLASS_NAME)+" cannot be assigned to "+ entityDef(CLASS_NAME))
                    case EntityType.INTERFACE =>
                      val interfaceName = entityDef(CLASS_NAME).asInstanceOf[String]
                      if(interfaceSubstitution(interfaceName, classMap(CLASS_NAME).asInstanceOf[String]))
                        classMap(FIELDS).asInstanceOf[mutableMapAny] ++= entityDef(FIELDS).asInstanceOf[mutableMapAny]
                        currentScopeFields += varName -> classMap
                        currentScopeFields(varName)
                      else
                        throw Exception("Class "+classMap(CLASS_NAME)+" cannot be assigned to interface:"+ interfaceName)

                  }

                case _:Any => throw Exception("Cannot declare instance of type variable")

              }



            case _:Any =>

              throw Exception("No Such class found")

          }

        /**
         * @param classDef: ClassDef that needs to be instantiated
         * instantiation of a class definition happens here
         * constructor invocation of current class and parent class occurs recursively
         */
        case NewObject(classDef: ClassDef) =>
          classDef.eval(scope) match {
            case classScope: mutableMapAny =>

              val className = classScope(CLASS_NAME).asInstanceOf[String]
              if(classInheritance(className)!=null) //checking for inheritance
                val classFields  = classScope(FIELDS).asInstanceOf[mutableMapAny]
                classFields += PARENT_INSTANCE -> NewObject(ClassDef(classInheritance(className))).eval(scope)
                //                currentScope(PARENT_INSTANCE) = NewObject(ClassDef(classInheritance(className))).eval()
                classFields += "parent"-> classFields(PARENT_INSTANCE).asInstanceOf[mutableMapAny](FIELDS)


              if(classScope(ENTITY_TYPE).asInstanceOf[EntityType] != EntityType.ABSTRACT_CLASS)
                //Invoking constructor
                val methods = classScope(METHODS).asInstanceOf[mutableMapAny]
                val constructor = methods(className).asInstanceOf[mutableMapAny]
                val constructorStatements:ArraySeq[BasicType] =constructor (METHOD_INSTRUCTIONS).asInstanceOf[ArraySeq[BasicType]]
                val constructorScope:mutableMapAny = constructor(FIELDS).asInstanceOf[mutableMapAny]

                constructorStatements.foreach(statement=> statement.asInstanceOf[Exp].eval(constructorScope))

              classScope
            case _:Any => throw Exception("invalid class definition")

          }

        /**
         * @param methodName: name of the method that needs invocation
         * @param parameters: method parameters if any
         * used to invoke methods in a given scope
         * also recursively checks for methods in parent class scopes if it does not exist in current scope
         * checks for access_type of method before returning (private, public)
         */
        case InvokeMethod(methodName, parameters) =>
          val instanceMethods = scope(METHODS).asInstanceOf[mutableMapAny]

          instanceMethods.get(methodName) match {
            case Some(methodScope:  mutableMapAny) =>

              if(scopeCall == SCOPE_CALL_TYPE_OUTER && scope(METHOD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, AccessType]](methodName) == AccessType.PRIVATE)
                throw Exception("method "+methodName+" is private")

              val methodScopeFields = methodScope(FIELDS).asInstanceOf[mutableMapAny]

              if(parameters.length == methodScopeFields.size -1 ){

                val parentExcludedScope = methodScopeFields.filter((k,v)=> k!="parent")
                //                                        val zippedParams:scala.collection.mutable.Iterable[(String, BasicType)] =  (parentExcludedScope.map((k,v) => k) zip parameters)
                methodScopeFields ++=  (parentExcludedScope.map((k,v) => k) zip parameters)

                val methodInstructions = methodScope(METHOD_INSTRUCTIONS).asInstanceOf[ArraySeq[BasicType]]
                if(methodInstructions.nonEmpty)
                  methodInstructions.take(methodInstructions.length - 1).foreach(statement =>statement.asInstanceOf[Exp].eval(methodScopeFields))
                methodInstructions.last.asInstanceOf[Exp].eval(methodScopeFields)
                //                  methodInstructions.foreach(println) //for testing purposes only, uncomment above line when ready
              }
              else
                throw Exception("Missing parameters")


            case None =>
              val instanceFields =  scope(FIELDS).asInstanceOf[mutableMapAny]
              instanceFields.get(PARENT_INSTANCE) match {
                case Some(parentInstance: mutableMapAny) =>
                  InvokeMethod(methodName, parameters).eval(parentInstance, SCOPE_CALL_TYPE_OUTER)
                case None => throw Exception(methodName +" not found")
              }
            //                  val parentInstance = instanceMap(PARENT_INSTANCE).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]

            //              }

          }
        case _: Any => throw Exception("invalid command")


      }

    /**
     *
     * @param classDef parent class from which current class inherits
     * used to establish inheritance between classes
     */
    implicit def Extends(entityDef: myDSLClassExt): BasicType =

      this match {
        case ClassDef(subClassName: String, args*) =>

          entityDef match{
            case ClassDef(superClassName, superClassArgs* ) =>

              this.eval().asInstanceOf[mutableMapAny]
              entityDef.eval().asInstanceOf[mutableMapAny]

              if(substitution(superClassName, subClassName) || substitution(subClassName, superClassName))
                throw Exception("Cyclic dependency between classes "+subClassName+" "+superClassName)
              else
                classInheritance+= subClassName -> superClassName

//            case AbstractClassDef(superClassName, superClassArgs*)=>
//              //TODO extending abstract classes
//              throw NotImplementedException()
            case AbstractClassDef(abstractClassName,abstractClassArgs *)=>


              val subClassDef = this.eval().asInstanceOf[mutableMapAny]
              val abstractClassDef = entityDef.eval().asInstanceOf[mutableMapAny]
              val abstractClassName = abstractClassDef(CLASS_NAME).asInstanceOf[String]
              val subClassName = subClassDef(CLASS_NAME).asInstanceOf[String]


              if(substitution(subClassName, abstractClassName) || substitution(abstractClassName, subClassName))
                throw Exception("Cyclic dependency between classes "+subClassName+" "+abstractClassName)

              val subClassDefMethodNames: List[String] = subClassDef(METHODS).asInstanceOf[mutableMapAny].map((classMethodDef,v)=> classMethodDef).toList


              val abstractMethodImplTypes =abstractClassDef(METHOD_IMPLEMENTATION_TYPES).asInstanceOf[mutable.Map[String, ImplementationType]]
              abstractClassDef(METHODS).asInstanceOf[mutableMapAny].foreach((interfaceMethodName, interfaceMethodDef) =>
                if(abstractMethodImplTypes(interfaceMethodName) == ImplementationType.ABSTRACT && !subClassDefMethodNames.contains(interfaceMethodName))
                  throw Exception(interfaceMethodName+" of "+abstractClassName+" is not implemented in class "+subClassName)
              )

              //TODO check method implementations

              classInheritance+= subClassName -> abstractClassName

            case _:Any => throw Exception("Invalid operation")
          }

        case InterfaceDef(name: String, args* )=>
          entityDef match{
            case InterfaceDef(superName, superArgs* ) =>
              val subInterface = this.eval().asInstanceOf[mutableMapAny]
              val superInterface= entityDef.eval().asInstanceOf[mutableMapAny]



              if(substitution(superName, name) || substitution(name, superName))
                throw Exception("Cyclic dependency between interfaces "+name+" "+superName)
              else
                classInheritance+= name -> superName

//              classInheritance+= name -> superName

              subInterface(FIELDS).asInstanceOf[mutableMapAny] += "parent" -> superInterface(FIELDS).asInstanceOf[mutableMapAny]
              subInterface+= "parent" -> superInterface





            case _:Any => throw Exception("Invalid operation "+ entityDef)
          }


        case _:Any => println("something else called implicit")

    }

    /**
     * @param interfaceDefinitions: accepts a list of interfaces to implement
     * @return
     */

    implicit def Implements (interfaceDefinitions: List[InterfaceDef]): BasicType =

//      println("implements param len "+interfaceDefinitions)
      this match {
        case ClassDef(name: String, args*) =>
          val classDef = this.eval().asInstanceOf[mutableMapAny]
          val concatenatedFields = mutable.Map[String, BasicType]()
          val validInterfacesNames = mutable.Map[String, BasicType]()
          for(interfaceDef <- interfaceDefinitions){
            val superInterface =  interfaceDef.eval().asInstanceOf[mutableMapAny]
            superInterface(FIELDS).asInstanceOf[mutableMapAny].foreach((k,v)=>  concatenatedFields.get(k) match{
              case Some(m)=> throw Exception("Duplicate field '"+k+"' found in multiple interfaces")
              case None =>
                concatenatedFields+= k->1
                validInterfacesNames += superInterface(CLASS_NAME).asInstanceOf[String] -> superInterface
//                validInterfacesDefs +=
            })

            val classDefMethodNames: List[String] = classDef(METHODS).asInstanceOf[mutableMapAny].map((classMethodDef,v)=> classMethodDef).toList

            superInterface(METHODS).asInstanceOf[mutableMapAny].foreach((interfaceMethodName,v) =>
              if(!classDefMethodNames.contains(interfaceMethodName))
                throw Exception(interfaceMethodName+" of "+superInterface(CLASS_NAME)+" is not implemented in class "+classDef(CLASS_NAME))
            )
          }

          validInterfacesNames.foreach((k,v)=> interfaceImpl(k) ++= List(name))

          classDef += IMPLEMENTED_INTERFACES ->validInterfacesNames.map((k,v)=> v).toList




        case _: Any => throw Exception("only ClassDef can call Implements")
      }


//
  @main def runExt(): Unit =

    import myDSLClassExt.*
    InterfaceDef("someInterface1", Field("a", AccessType.PUBLIC, Value(20)), Method("method1", AccessType.PUBLIC, ImplementationType.ABSTRACT, List())).eval()
    ClassDef("someClassName1",
      Field("x", AccessType.PRIVATE, Value(47)),
      Constructor(Value(1)),
      Method(
        "method1",
        AccessType.PUBLIC,
        ImplementationType.CONCRETE,
        List(),
        Value(1)
      )
    ).eval()

    AbstractClassDef("AbsClass1",
      Field("y", AccessType.PUBLIC, Value(10)),
      Method("method1",
        AccessType.PUBLIC,
        ImplementationType.ABSTRACT,
        List()
      )
    ).eval()
    ClassDef("someClassName1") Extends AbstractClassDef("AbsClass1")
    ClassDef("someClassName1") Implements List(InterfaceDef("someInterface1"))

    DeclareInstance(InterfaceDef("someInterface1"),"inst1").eval()
    AssignInstance(NewObject(ClassDef("someClassName1")), "inst1").eval()



/**
    CLASS SCOPE MAP STRUCTURE
    CLASSDEF/INSTANCE: {
      TYPES: Map(string, string)
      ACCESS Map(String, String)
      FIELDS: {variables, instances}
      METHOD: {
        m1: {
          FIELDS: Map(String, Any) contains parameter
          INSTRUCTIONS: []
        }
        constructor : method but with class name
      }
      CLASSDEF: structure of CLASSDEF ^^^





    }
    */