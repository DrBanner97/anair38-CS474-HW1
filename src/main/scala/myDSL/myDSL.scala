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

    def eval(scope: scala.collection.mutable.Map[String, Any] = bindingScope): Either[BasicType, BasicType] =
      this match {
        case Value(i) => Right(i)
        case Var(name) =>
          scope.get(name) match {
            case None =>
            // moving to outer-scope(if available) when variable is not found in current scope
            scope.get("parent") match {
                case Some(parentScope: scala.collection.mutable.Map[String, Any]) => this.eval(parentScope)
                case None =>
                  Left(this)
              }

            case Some(value) =>
              Right(value)
          }

        case Check(exp1: Exp, exp2: Exp) =>
          val exp1Eval = exp1.eval(scope)
          val exp2Eval = exp2.eval(scope)

          (exp1Eval,exp2Eval) match {

            case (Right(exp1Value), Right(exp2Value)) => Right(exp1Value == exp2Value)
            case (Left(exp1Error), Right(exp2Value)) => Left(Check(exp1, Value(exp2Value))) //what if Value is passed?
            case (Right(exp1Value), Left(exp2Error)) => Left(Check(Value(exp1Value), exp2))
            case (Left(exp1Error), Left(exp2Error)) => Left(Check(exp1, exp2))
          }

//          exp1Eval == exp2Eval

        case Assign(varName: String, value: Exp) =>

          scope.get(varName) match {
            // moving to outer-scope(if available) when variable is not found in current scope
            case None => scope("parent") match {
              case None => Left(Assign(varName, value)) //partial eval
              case parentScope: scala.collection.mutable.Map[String, Any] =>
                this.eval(parentScope)
            }
            case _: Any =>
//             value.eval(scope)
              scope += varName -> value.eval(scope).merge
              Right(scope(varName))

          }
        //only allow declaration if variable doesn't already exist
        case DeclareVar(varName: String, value: Exp) => scope.get(varName) match {
          case None =>
            scope += varName -> value.eval(scope).merge
            Right(scope)
          case _: Any => throw Error("Variable already exists") // no partial evaluation here
        }

        //creates and returns a scala.collection.immutable.Set and initializes with any number of Values
        case CreateSet(args*) =>
          Right(Set[Any]() ++ args.map(arg => arg.asInstanceOf[Exp].eval(scope).merge).toSet)

        //insert only when set evaluates to type Set
        case Insert(set: Exp, args*) =>
          val setEval = set.eval(scope)
          setEval match {
            case Right(immutableSet: Set[Any]) =>
             Right( immutableSet ++ args.map(arg => arg.asInstanceOf[Exp].eval(scope).merge).toSet)

            case Left(value): Any => Left(this)
          }

        //delete only when set evaluates to type Set
        case Delete(set: Exp, args*) =>
          val setEval = set.eval(scope)
          setEval match {
            case Right( immutableSet: Set[Any]) =>
              Right(immutableSet -- args.map(arg => arg.asInstanceOf[Exp].eval(scope).merge).toSet)

            case Left(value) => Left(this)
          }

          //only evaluates if both the arguments evaluate to Set
        case Union(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (Right(immutableSet1: Set[Any]), Right(immutableSet2: Set[Any])) =>
              Right(immutableSet1.union(immutableSet2))
            case (Left(unevaluatedSet1), Right(immutableSet2: Set[Any])) => Left(Union(set1, Value(immutableSet2)))
            case (Right(immutableSet1: Set[Any]), Left(unEvaluatedSet2)) => Left(Union(Value(immutableSet1), set2))
            case (Left(unevaluatedSet1), Left(unEvaluatedSet2)) => Left(this)

//            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Diff(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (Right(immutableSet1: Set[Any]), Right(immutableSet2: Set[Any])) =>
              Right(immutableSet1.diff(immutableSet2))
            case (Left(unevaluatedSet1), Right(immutableSet2: Set[Any])) => Left(Diff(set1, Value(immutableSet2)))
            case (Right(immutableSet1: Set[Any]), Left(unEvaluatedSet2)) => Left(Diff(Value(immutableSet1), set2))
            case (Left(unevaluatedSet1), Left(unEvaluatedSet2)) => Left(this)
//            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Intersection(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (Right(immutableSet1: Set[Any]), Right(immutableSet2: Set[Any])) =>
              Right(immutableSet1.intersect(immutableSet2))
            case (Left(unevaluatedSet1), Right(immutableSet2: Set[Any])) => Left(Intersection(set1, Value(immutableSet2)))
            case (Right(immutableSet1: Set[Any]), Left(unEvaluatedSet2)) => Left(Intersection(Value(immutableSet1), set2))
            case (Left(unevaluatedSet1), Left(unEvaluatedSet2)) => Left(this)
//            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case SymmetricDiff(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (Right(immutableSet1: Set[Any]), Right(immutableSet2: Set[Any])) =>
              Right(immutableSet1.union(immutableSet2).diff(immutableSet1.intersect(immutableSet2)))
            case (Left(unevaluatedSet1), Right(immutableSet2: Set[Any])) => Left(SymmetricDiff(set1, Value(immutableSet2)))
            case (Right(immutableSet1: Set[Any]), Left(unEvaluatedSet2)) => Left(SymmetricDiff(Value(immutableSet1), set2))
            case (Left(unevaluatedSet1), Left(unEvaluatedSet2)) => Left(this)
//            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        //only evaluates if both the arguments evaluate to Set
        case Product(set1: Exp, set2: Exp) =>
          (set1.eval(scope), set2.eval(scope)) match {
            case (Right(immutableSet1: Set[Any]), Right(immutableSet2: Set[Any])) =>
              Right(immutableSet1.flatMap(i1 => immutableSet2.map(i2 => (i1, i2))))
            case (Left(unevaluatedSet1), Right(immutableSet2: Set[Any])) => Left(Product(set1, Value(immutableSet2)))
            case (Right(immutableSet1: Set[Any]), Left(unEvaluatedSet2)) => Left(Product(Value(immutableSet1), set2))
            case (Left(unevaluatedSet1), Left(unEvaluatedSet2)) => Left(this)
//            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
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

          Right(exp.map(e => e.eval(newAnonScope).merge))
//          Right(scope)

//          exp.eval(newAnonScope)


        case SetMacro(macroName, exp: Exp) =>
          scope.get(MACRO_KEY) match {
            case Some(scopeMacroMap: scala.collection.mutable.Map[String, Any]) =>
              //possibly re-writing  macros
              scopeMacroMap.put(macroName, exp)
              Right(scopeMacroMap(macroName))
            case None: Any =>
              throw Error("Could not find specified macro ")

          }

        case GetMacro(macroName) =>
          scope.get(MACRO_KEY) match {
            case Some(scopeMacroMap: scala.collection.mutable.Map[String, Any] )=>
              scopeMacroMap.get(macroName) match {
                case Some(value: Exp) =>
                  value.eval(scope)
                case None => Left(this)
              }
            case None => throw Error("Could not find specified macro")
          }
        case If(condition: Exp, thenClause, elseClause) =>
          val conditionEval = condition.eval(scope)
          conditionEval match{
            case Right(conditionBool: Boolean) =>
              if(conditionBool)
                thenClause.eval(scope)
              else
                elseClause.eval(scope)
            case Left(value: Exp) =>

              Left(If(value,
                AnonScope(
                  thenClause.eval(scope).merge
                    .asInstanceOf[scala.collection.immutable.ArraySeq[BasicType]]
                    .map[Exp] {
                      case exp: Exp => exp
                      case e => Value(e)
                    }:_*
                ),
                AnonScope(
                  elseClause.eval(scope).merge
                  .asInstanceOf[scala.collection.immutable.ArraySeq[BasicType]]
                    .map[Exp] {
                      case exp: Exp => exp
                      case e => Value(e)
                    }:_*
                ))
              )
//              Left(this)
          }
      }

  trait myDSLExp:
    def map(f: Exp => Exp): Exp

  case class myDSLExpMonad(set: Exp) extends myDSLExp{
    override def map(f: Exp => Exp): Exp = {
      f(set)
    }
  }


  @main def runExp(): Unit =
    import Exp.*










