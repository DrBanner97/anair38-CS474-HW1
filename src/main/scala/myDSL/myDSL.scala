package myDSL

import com.sun.jdi.InvalidTypeException

import java.util.NoSuchElementException
import scala.util.Random

object myDSL:

  private val MACRO_KEY: String = "macro"
  private val exceptionClasses: mutableMap =  scala.collection.mutable.Map()
  private val bindingScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map()
  bindingScope += MACRO_KEY -> scala.collection.mutable.Map[String, Any]()

  type BasicType = Any
  type mutableMap = scala.collection.mutable.Map[String, Any]

  enum Exp:
    case Value(input: BasicType) // to pass in values
    case Var(name: String) // to retrieve variables by name froms scope
    case Check(exp1: Exp, exp2: Exp)
    case DeclareVar(varName: String, value: Exp) // to declare variables in given scope
    case Assign(varName: String, value: Exp)// to assign/re-assign values to an existing variable
    case CreateSet(args: Any*) // to create a set with some values
    case Insert(set: Exp, args: Any*) // to insert into existing set
    case Delete(set: Exp, args: Any*) //to delete items from an existing set
    case Union(set1: Exp, set2: Exp) //to perform Union operation on two sets
    case Intersection(set1: Exp, set2: Exp) //to perform Intersection operation on two sets
    case Diff(set1: Exp, set2: Exp) //to perform Set Difference operation on two sets
    case SymmetricDiff(set1: Exp, set2: Exp) //to perform Symmetric Difference operation on two sets
    case Product(set1: Exp, set2: Exp) //to perform Cartesian Product on two sets
    case Scope(scopeName: String, exp: Exp) //to define/evaluate expressions in a user-defined named scope
    case AnonScope(exp: Exp*) //to define expressions in a user-defined  un-named scope
    case SetMacro(macroName: String, exp: Exp) //to define macros
    case GetMacro(macroName: String) // to fetch/evaluate macro expressions
    case If(condition: Exp, thenClause: AnonScope, elseClause: AnonScope)

    //attribute scope allows switching to different scopes
    def eval(scope: scala.collection.mutable.Map[String, Any] = bindingScope): BasicType =
      this match {
        case Value(i) => i
        case Var(name) =>
          scope.get(name) match {
            case None =>
            // moving to outer-scope(if available) when variable is not found in current scope
            scope.get("parent") match {
                case Some(parentScope: scala.collection.mutable.Map[String, Any]) => this.eval(parentScope)
                case None => throw NoSuchElementException()
              }

            case Some(value) => value
          }

        case Check(exp1: Exp, exp2: Exp) =>
          val exp1Eval = exp1.eval(scope)
          val exp2Eval = exp2.eval(scope)

          exp1Eval == exp2Eval


        case Assign(varName: String, value: Exp) =>

          scope.get(varName) match {
            // moving to outer-scope(if available) when variable is not found in current scope
            case None => scope("parent") match {
              case None => throw Error("Variable could not be assigned: Variable does not exist")
              case parentScope: scala.collection.mutable.Map[String, Any] =>
                this.eval(parentScope)
            }
            case _: Any => scope += varName -> value.eval(scope)

          }
        //only allow declaration if variable doesn't already exist
        case DeclareVar(varName: String, value: Exp) => scope.get(varName) match {
          case None => scope += varName -> value.eval(scope)
          case _: Any => throw Error("Variable already exists")
        }

        //creates and returns a scala.collection.immutable.Set and initializes with any number of Values
        case CreateSet(args*) => Set[Any]() ++ args.map(arg => arg.asInstanceOf[Exp].eval(scope)).toSet

        //insert only when set evaluates to type Set
        case Insert(set: Exp, args*) =>
          val setEval = set.eval(scope)
          setEval match {
            case immutableSet: Set[Any] =>
              immutableSet ++ args.map(arg => arg.asInstanceOf[Exp].eval(scope)).toSet

            case _: Any => throw InvalidTypeException("invalid parameter type: parameter set should evaluate to a Set")
          }

        //delete only when set evaluates to type Set
        case Delete(set: Exp, args*) =>
          val setEval = set.eval(scope)
          setEval match {
            case immutableSet: Set[Any] =>
              immutableSet -- args.map(arg => arg.asInstanceOf[Exp].eval(scope)).toSet

            case _: Any => throw InvalidTypeException("invalid parameter type: parameter set should evaluate to a Set")
          }

          //only evaluates if both the arguments evaluate to Set
        case Union(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.union(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Diff(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.diff(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Intersection(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.intersect(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case SymmetricDiff(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.union(immutableSet2).diff(immutableSet1.intersect(immutableSet2))

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Product(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.flatMap(i1 => immutableSet2.map(i2 => (i1, i2)))

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case Scope(scopeName, exp) =>
          scope.get(scopeName) match {
            //creating the scope in case it doesn't exist
            case None =>
              val newScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map()
              //adding reference to parent scope
              newScope += "parent" -> scope
              //adding map that stores Macro definitions withing the scope
              newScope += MACRO_KEY -> scala.collection.mutable.Map[String, Any]()
              scope += scopeName -> newScope

              exp.eval(newScope)


            case Some(localScope: scala.collection.mutable.Map[String, Any]) =>
              exp.eval(localScope)

          }

        case AnonScope(exp*) =>
          val newAnonScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map()
          newAnonScope += MACRO_KEY -> scala.collection.mutable.Map[String, Any]()
          newAnonScope += "parent" -> scope
          //pairing anonymous scope with random key
          scope += (Random.alphanumeric take 10).mkString -> newAnonScope

          exp.foreach(e => e.eval(newAnonScope))

//          exp.eval(newAnonScope)


        case SetMacro(macroName, exp: Exp) =>
          scope.get(MACRO_KEY) match {
            case Some(scopeMacroMap) =>
              //possibly re-writing  macros
              scopeMacroMap.asInstanceOf[scala.collection.mutable.Map[String, Any]].put(macroName, exp)
            case anyElse: Any =>
              throw Error("Could not find specified macro ")

          }

        case GetMacro(macroName) =>
          scope(MACRO_KEY) match {
            case scopeMacroMap: scala.collection.mutable.Map[String, Any] =>
              scopeMacroMap(macroName) match {
                case value: Exp =>
                  value.eval(scope)
                case None => throw Error("Could not evaluate Macro")
              }
            case None => throw Error("Could not find specified macro")
          }
        case If(condition: Exp, thenClause, elseClause) =>
          val conditionEval = condition.eval(scope)
          conditionEval match{
            case conditionBool: Boolean =>
              if(conditionBool)
                AnonScope(thenClause).eval(scope)
              else
                AnonScope(elseClause).eval(scope)
            case _:Any =>
              throw Exception("condition in If should evaluate to Boolean")
          }
      }

  @main def runExp(): Unit =
    import Exp.*
//    DeclareVar("a", CreateSet(Value(1), Value(2), Value(3))).eval()
//    DeclareVar("b", CreateSet(Value(3), Value(4), Value(5))).eval()
//    Assign("a",Insert(Var("a"), Value(4))).eval()
//    println(Var("a").eval())
//    DeclareVar("a", Value(5)).eval()
//    DeclareVar("b", Value(1)).eval()
//    If(Var("a").eval() == 2,Assign("b", CreateSet(Value(1), Value(2), Value(3))), Assign("b", CreateSet(Value(2), Value(3), Value(4))) ).eval()
//    println(Var("b").eval())

//    DeclareVar("x", Value(3)).eval()
//    println(Check(Var("a"), Value(3)).eval())

//      AnonScope(DeclareVar("x", CreateSet(Value(1),Value(2),Value(3))), DeclareVar("y", CreateSet(Value(3),Value(4),Value(5))), Union(Var("x"), Var("y"))).eval()
      DeclareVar("x", Value(2)).eval()

      If(Check(Var("x"), Value(1)),
        AnonScope(DeclareVar("y", CreateSet(Value(1),Value(2),Value(3))), Assign("x", CreateSet(Value(3),Value(4),Value(5)))),
        AnonScope(Assign("x", Value(10))),
      ).eval()

      println(Var("x").eval())








