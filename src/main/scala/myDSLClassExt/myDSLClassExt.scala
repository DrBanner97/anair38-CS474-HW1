package myDSLClassExt
import scala.language.implicitConversions
import myDSL.myDSL.Exp
import myDSL.myDSL.Exp.*

import scala.collection.immutable.ArraySeq

object myDSLClassExt:
  private val FIELDS: String = "fields"
  private val FIELD_TYPES: String = "field_types"
  private val FIELD_ACCESS_TYPES: String = "field_access_types"
  private val ACCESS_TYPE_PUBLIC: String = "public"
  private val ACCESS_TYPE_PRIVATE: String = "private"
  private val METHODS: String = "methods"
  private val METHOD_ACCESS_TYPES : String = "method_access_type"
  private val CLASS_DEFINITION: String = "class_definition"
  private val CLASS_NAME: String = "class_name"
  private val METHOD_SCOPE: String = "method_scope"
  private val METHOD_INSTRUCTIONS: String = "method_instructions"
  private val FIELD_TYPE_VARIABLE: String = "variable"
  private val PARENT_INSTANCE: String = "parent_instance"
  private val SCOPE_CALL_TYPE_INNER: String = "inner"
  private val SCOPE_CALL_TYPE_OUTER: String = "outer"


  type BasicType = Any
  private val classInheritance: scala.collection.mutable.Map[String, String] = scala.collection.mutable.Map() // used to keep track of class inheritance chain
  classInheritance += FIELD_TYPE_VARIABLE -> null
  private val outerScope: scala.collection.mutable.Map[String, BasicType] = scala.collection.mutable.Map()
  outerScope +=  FIELDS -> scala.collection.mutable.Map[String, BasicType]() //kinda redundant
  outerScope +=  FIELD_TYPES -> scala.collection.mutable.Map[String, String]() //kinda redundant
  outerScope +=  METHODS -> scala.collection.mutable.Map[String, String]() //kinda redundant
  outerScope +=  CLASS_DEFINITION -> scala.collection.mutable.Map[String, String]()


  //noinspection ScalaDocUnknownParameter
  enum myDSLClassExt:
    case ClassDef(name: String, args: myDSLClassExt*) //to define and reference class definition
    case Field(varName: String, accessType: String  ,value: BasicType*) //to define Fields within ClassDef
    case GetField(fieldName: String)//to fetch fields from an instance of a class
    case Constructor(params:Exp*) //to define constructor within ClassDef
    case Method(methodName: String ,accessType: String,parameters: List[BasicType],params:BasicType*) //to define method within ClassDef
    case Instance(instanceName: String, operation: myDSLClassExt) //used to reference an instance of class and perform instance based operation (get fields, methods and inner classes)
    case DeclareInstance(classDef: ClassDef, varName: String) // used to declare an instance
    case AssignInstance(classDef: myDSLClassExt, varName: String) //uses previous declaration to assign a new instance
    case NewObject(classDef: ClassDef) // creates an instance of a ClassDef, invoked class constructor
    case InvokeMethod( methodName: String, parameters: List[BasicType]) //invokes a method in a given instance

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

    def eval(scope: scala.collection.mutable.Map[String, BasicType] = outerScope, scopeCall: String = SCOPE_CALL_TYPE_INNER): BasicType =

      this match {
        /**
         * @param name: variable name of the field
         * fetches field from given scope. with inherited classes GetField looks for field in parent class if not present
         */
        case GetField(name: String) =>

          val scopeFields = scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
          scopeFields.get(name) match {
            case Some(value: BasicType) =>
              if(scopeCall == SCOPE_CALL_TYPE_OUTER && scope(FIELD_ACCESS_TYPES).asInstanceOf[ scala.collection.mutable.Map[String, String]](name) == ACCESS_TYPE_PRIVATE)
                throw Exception("variable "+ name+" is private")
              else
                value
            case None => scopeFields.get(PARENT_INSTANCE) match {
              case Some(parentInstance: scala.collection.mutable.Map[String, BasicType] ) =>
                GetField(name).eval(parentInstance, SCOPE_CALL_TYPE_OUTER)
              case None => throw Exception("Field "+name+" not found")
            }

          }

        /**
         * @param instanceName: variable name of the instance
         * evaluates operation in the scope of instance
         */
        case Instance(instanceName, operation)=>

          val instance=GetField(instanceName).eval(scope).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]

          //          val instance = scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, String]](instanceName)
          operation.eval(instance)


        /**
         *  @param name: name of class
         *  @param args: class definitions like Constructor, Methods, Field, ClassDef(inner classes) go here
         *  used for class definition or fetching class definition (when args is not provided)
         */
        case ClassDef(name: String, args*) =>


          if(name == FIELD_TYPE_VARIABLE)
            throw Exception("class name cannot be "+FIELD_TYPE_VARIABLE)

          if(args.length>0) {
            val classScope: scala.collection.mutable.Map[String, BasicType] = scala.collection.mutable.Map()
            classScope +=  FIELDS -> scala.collection.mutable.Map[String, BasicType]() //kinda redundant
            classScope +=  FIELD_TYPES -> scala.collection.mutable.Map[String, String]() //kinda redundant
            classScope +=  FIELD_ACCESS_TYPES -> scala.collection.mutable.Map[String, String]() //kinda redundant
            classScope +=  METHODS -> scala.collection.mutable.Map[String, BasicType]() //kinda redundant
            classScope +=  METHOD_ACCESS_TYPES -> scala.collection.mutable.Map[String, String]() //kinda redundant
            classScope +=  CLASS_DEFINITION -> scala.collection.mutable.Map[String, BasicType]()
            //            val classBindingScope: scala.collection.mutable.Map[String, BasicType] = scala.collection.mutable.Map()
            //            classMap += CLASS_SCOPE -> classBindingScope
            classScope += CLASS_NAME -> name

            for( arg <- args){
              arg match {

                /**
                 * @param params*: ArraySeq of Exp (Set theory) operations to execute in order
                 * invoked when NewObject is called
                 * returns evaluation of last statement
                 */
                case Constructor(params*) =>
                  val constructorScope: scala.collection.mutable.Map[String, BasicType] = scala.collection.mutable.Map()
                  constructorScope += "parent" -> classScope
                  constructorScope += FIELDS -> scala.collection.mutable.Map[String, String]()
                  constructorScope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += "parent"-> classScope(FIELDS)
                  constructorScope += FIELD_TYPES -> scala.collection.mutable.Map[String, String]()
                  constructorScope += METHOD_INSTRUCTIONS -> params

                  classScope(METHODS).asInstanceOf[scala.collection.mutable.Map[String, Any]] += name -> constructorScope

                /**
                 * @param methodName: name of the method to define
                 * @param accessType: access modifiers, accepts string values "public" and "private"
                 * @param params*: ArraySeq of Exp (Set theory) operations to execute in order
                 * invoked using InvokeMethod
                 * returns evaluation of last statement
                 */
                case Method(methodName,accessType,parameters, params*) =>
                  if (methodName == name) throw Exception("method name cannot be the same as class name")
                  else
                    val methodScope: scala.collection.mutable.Map[String, BasicType] = scala.collection.mutable.Map()
                    methodScope += "parent" -> classScope
                    methodScope += FIELDS -> (scala.collection.mutable.Map[String, String]() ++ parameters.map(param=> param.asInstanceOf[String] ->0))
                    methodScope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += "parent"-> classScope(FIELDS)
                    methodScope += METHOD_INSTRUCTIONS -> params
                    classScope(METHODS).asInstanceOf[scala.collection.mutable.Map[String, Any]] += methodName -> methodScope
                    classScope(METHOD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, String]] += methodName -> accessType

                /**
                 * @param varName: name of the field
                 * @param accessType: access modifiers, accepts string values "public" and "private"
                 * @param value*: optional value field
                 *
                 * defines instance variable in a class scope
                 */
                case Field(varName, accessType ,value*) =>
                  val classScopeFields = classScope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
                  if (value.length == 0)
                    Exp.DeclareVar(varName = varName, value = null).eval(classScopeFields)
                  else
                    value(0) match {
                      case exp: Exp => Exp.DeclareVar(varName = varName, value = exp).eval(classScopeFields)
                      case _: Any => throw Exception("Invalid field values")
                    }
                  classScope(FIELD_TYPES).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += varName -> FIELD_TYPE_VARIABLE
                  classScope(FIELD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, String]] += varName -> accessType

                /**
                 *  @param name: name of class
                 *  @param args: class definitions like Constructor, Methods, Field, ClassDef(inner classes) go here
                 *  used for class definition or fetching class definition (when args is not provided)
                 */
                case ClassDef (name: String, args*) =>
                  classScope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += name -> ClassDef(name, args*).eval(classScope)

                case _: Any => throw Exception("Invalid item found")
              }

            }

            scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String,BasicType]] += name -> classScope
            classInheritance += name -> null
          }
          //          val classDefs = scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, String]]


          scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, String]](name)

        /**
         * @param classDef: ClassDef type of the instance
         * @param varName: desired name of instance
         * declares a variable instance of type classDef
         */
        case DeclareInstance(classDef, varName) =>

          val classDefs = scope(CLASS_DEFINITION).asInstanceOf[scala.collection.mutable.Map[String, String]]


          val classScope = classDef.eval(scope).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
          scope(FIELD_TYPES).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += varName -> classScope(CLASS_NAME).asInstanceOf[String]
          scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += varName -> null
          classScope

        /**
         * @param classDef: ClassDef type of the instance
         * @param varName: name of existing instance variable
         * binds instance of ClassDef to varName
         */
        case AssignInstance(classDef, varName) =>


          classDef.eval(scope) match{
            case classMap: scala.collection.mutable.Map[String, BasicType] =>
              val instanceType  = scope(FIELD_TYPES).asInstanceOf[scala.collection.mutable.Map[String, String]]

              if (substitution(instanceType(varName), classMap(CLASS_NAME).asInstanceOf[String]))
                scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]] += varName -> classMap
              else
                throw Exception("Class "+classMap(CLASS_NAME)+" cannot be assigned to "+ instanceType(varName))

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
            case classScope: scala.collection.mutable.Map[String, BasicType] =>

              val className = classScope(CLASS_NAME).asInstanceOf[String]
              if(classInheritance(className)!=null) //checking for inheritance
                val classFields  = classScope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
                classFields += PARENT_INSTANCE -> NewObject(ClassDef(classInheritance(className))).eval(scope)
                //                classScope(PARENT_INSTANCE) = NewObject(ClassDef(classInheritance(className))).eval()
                classFields += "parent"-> classFields(PARENT_INSTANCE).asInstanceOf[scala.collection.mutable.Map[String, BasicType]](FIELDS)

              //Invoking constructor
              val methods = classScope(METHODS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
              val constructor = methods(className).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
              val constructorStatements:ArraySeq[BasicType] =constructor (METHOD_INSTRUCTIONS).asInstanceOf[ArraySeq[BasicType]]
              val constructorScope:scala.collection.mutable.Map[String, BasicType] = constructor(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]

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
          val instanceMethods = scope(METHODS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]

          instanceMethods.get(methodName) match {
            case Some(methodScope:  scala.collection.mutable.Map[String, BasicType]) =>

              if(scopeCall == SCOPE_CALL_TYPE_OUTER && scope(METHOD_ACCESS_TYPES).asInstanceOf[scala.collection.mutable.Map[String, String]](methodName) == ACCESS_TYPE_PRIVATE)
                throw Exception("method "+methodName+" is private")

              val methodScopeFields = methodScope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]

              if(parameters.length == methodScopeFields.size -1 ){

                val parentExcludedScope = methodScopeFields.filter((k,v)=> k!="parent")
                //                                        val zippedParams:scala.collection.mutable.Iterable[(String, BasicType)] =  (parentExcludedScope.map((k,v) => k) zip parameters)
                methodScopeFields ++=  (parentExcludedScope.map((k,v) => k) zip parameters)

                val methodInstructions = methodScope(METHOD_INSTRUCTIONS).asInstanceOf[ArraySeq[BasicType]]

                methodInstructions.take(methodInstructions.length - 1).foreach(statement =>statement.asInstanceOf[Exp].eval(methodScopeFields))

                methodInstructions.last.asInstanceOf[Exp].eval(methodScopeFields)
                //                  methodInstructions.foreach(println) //for testing purposes only, uncomment above line when ready
              }
              else
                throw Exception("Missing parameters")


            case None =>
              val instanceFields =  scope(FIELDS).asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
              instanceFields.get(PARENT_INSTANCE) match {
                case Some(parentInstance: scala.collection.mutable.Map[String, BasicType]) =>
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
    implicit def Extends(classDef: myDSLClassExt): BasicType =

      this match {
        case ClassDef(name: String, args*) =>

          val subClassDefinition = this.eval().asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
          val superClassDefinition = classDef.eval().asInstanceOf[scala.collection.mutable.Map[String, BasicType]]
          classInheritance(subClassDefinition(CLASS_NAME).asInstanceOf[String]) = superClassDefinition(CLASS_NAME).asInstanceOf[String]

        case _:Any => println("something else called implicit")

    }


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