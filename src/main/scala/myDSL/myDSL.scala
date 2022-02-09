package myDSL

import com.sun.jdi.InvalidTypeException

import scala.util.Random

object myDSL:

  private val MACRO_KEY: String = "macro"

  private val bindingScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map()
  bindingScope += MACRO_KEY -> scala.collection.mutable.Map[String, Any]()

  type BasicType = Any

  enum Exp:
    case Value(input: BasicType)
    case Var(name: String)
    case CreateSet(args: Any*)
    case Assign(varName: String, value: Exp)
    case Insert(set: Exp, args: Any*)
    case Delete(set: Exp, args: Any*)
    case Union(set1: Exp, set2: Exp)
    case Intersection(set1: Exp, set2: Exp)
    case Diff(set1: Exp, set2: Exp)
    case SymmetricDiff(set1: Exp, set2: Exp)
    case Product(set1: Exp, set2: Exp)
    case Scope(scopeName: String, exp: Exp)
    case AnonScope(exp: Exp)
    case SetMacro(macroName: String, exp: Exp)
    case GetMacro(macroName: String)


    def eval(scope: scala.collection.mutable.Map[String, Any] = bindingScope): BasicType =
      this match {
        case Value(i) => i
        case Var(name) => scope(name)
        case Assign(varName: String, value: Exp) =>
          scope += varName -> value.eval()
        //          scope(varName)

        case CreateSet(args*) => Set[Any]() ++ args.map(arg => arg.asInstanceOf[Exp].eval()).toSet
        case Insert(set: Exp, args*) =>
          val setEval = set.eval()
          setEval match {
            case immutableSet: Set[Any] =>
              immutableSet ++ args.map(arg => arg.asInstanceOf[Exp].eval()).toSet

            case _: Any => throw InvalidTypeException("invalid parameter type: parameter set should evaluate to a Set")
          }

        case Delete(set: Exp, args*) =>
          val setEval = set.eval()
          setEval match {
            case immutableSet: Set[Any] =>
              immutableSet -- args.map(arg => arg.asInstanceOf[Exp].eval()).toSet

            case _: Any => throw InvalidTypeException("invalid parameter type: parameter set should evaluate to a Set")
          }

        case Union(set1: Exp, set2: Exp) =>
          (set1.eval(), set2.eval()) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.union(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case Diff(set1: Exp, set2: Exp) =>
          (set1.eval(), set2.eval()) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.diff(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case Intersection(set1: Exp, set2: Exp) =>
          (set1.eval(), set2.eval()) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.intersect(immutableSet2)

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case SymmetricDiff(set1: Exp, set2: Exp) =>
          (set1.eval(), set2.eval()) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.union(immutableSet2).diff(immutableSet1.intersect(immutableSet2))

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case Product(set1: Exp, set2: Exp) =>
          (set1.eval(), set2.eval()) match {
            case (immutableSet1: Set[Any], immutableSet2: Set[Any]) =>
              immutableSet1.flatMap(i1 => immutableSet2.map(i2 => (i1, i2)))

            case _: Any => throw InvalidTypeException("invalid parameter(s) type")
          }

        case Scope(scopeName, exp) =>
          scope.get(scopeName) match {
            case None =>
              val newScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map() ++ scope

              //              newScope ++= scope
              //              newScope += MACRO_KEY -> scala.collection.mutable.Map[String, Any]()
              scope += scopeName -> newScope

              exp.eval(newScope)

            case Some(value) =>
              exp.eval(value.asInstanceOf[scala.collection.mutable.Map[String, Any]])

          }

        case AnonScope(exp) =>
          val newScope: scala.collection.mutable.Map[String, Any] = scala.collection.mutable.Map() ++ scope
          scope += (Random.alphanumeric take 10).mkString -> newScope
          exp.eval(newScope)

        //scoping with macro
        //macro duplicates
        case SetMacro(macroName, exp: Exp) =>
          scope.get(MACRO_KEY) match {
            case Some(scopeMacroMap) =>
              //possibly re-writing  macros
              scopeMacroMap.asInstanceOf[scala.collection.mutable.Map[String, Any]].put(macroName, exp)
            case anyElse: Any =>
              throw Error("Could not find specified macro ")

          }

        case GetMacro(macroName) =>
          scope.get(MACRO_KEY) match {
            case Some(scopeMacroMap) =>
              scopeMacroMap.asInstanceOf[scala.collection.mutable.Map[String, Any]].get(macroName) match {
                case Some(value) => value.asInstanceOf[Exp].eval()
                case _: Any => throw Error("Could not evaluate Macro")
              }
            case _: Any => throw Error("Could not find specified macro")
          }


      }

  @main def runExp(): Unit =
    import Exp.*
    Assign("a", CreateSet(Value(1),Value(2),Value(3))).eval()
    Assign("b", CreateSet(Value(3),Value(4),Value(5))).eval()

    println(Product(Var("a"), Var("b")).eval())

    //access outer scope stuff
    Scope("scope1", Assign("c", CreateSet(Value(7),Value(8),Value(9)))).eval()
    //TODO test macro input (eval/no eval)
    SetMacro("four", Value(4)).eval()

    Assign("a", Insert(Var("a"), GetMacro("four"))).eval()


    AnonScope(Assign("x", Var("a"))).eval()
    println(Scope("scope1", Var("c")).eval())
//    println(Insert(Var("a"), Var("b")).eval())
//    println(GetMacro("one").eval())

//    Scope("s1", SetMacro("two", Value(2))).eval()
//    println(Scope("s1", GetMacro("two")).eval())
//    println(GetMacro("two").eval())

//    println(Union(Var("a"), Var("b")).eval())
//    println(Diff(Var("a"), Var("b")).eval())
//    println(Diff(Var("b"), Var("a")).eval())
//    println(Intersection(Var("a"), Var("b")).eval())
//    println(SymmetricDiff(Var("a"), Var("b")).eval())
//    println(Product(Var("a"), Var("b")).eval())
//
////    println(Var("c").eval())
//    println(Scope("scope1", Value(1)).eval())





