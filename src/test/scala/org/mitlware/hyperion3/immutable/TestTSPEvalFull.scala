package org.mitlware.hyperion3.immutable

import cats._
import cats.data._
import cats.implicits._

import monocle.Lens

//import scalaz.State
//import scalaz.Lens

import org.mitlware.Diag

import org.mitlware.hyperion3.immutable._

import org.mitlware.support.lang.BadFormatException

import org.mitlware.support.lang.UnsupportedFormatException
import org.mitlware.support.math.Vec2

import org.mitlware.solution.permutation.ArrayForm
import org.mitlware.problem.tsp.TSP

import org.junit.Test

import java.io._
import java.nio.charset.Charset
import java.nio._
import java.nio.file._

import org.junit.Assert._ 

//////////////////////////////////////////////////////////////////////

class TestTSPEvalFull {

	@Test
	def testIteratedPerturbation: Unit = {
		
		val seed = 0xDEADBEEF
	  val maxIter = 10000	  
	  
    ///////////////////////////////
	  
//		val path = System.getProperty( "user.dir" ) + "/resources/" + "unitTest.tsp";
		val path = System.getProperty( "user.dir" ) + "/resources/" + "wi29.tsp";	  
		val is = new FileInputStream( path );
		val tsp = new TSP.TSPLibInstance( is );

//		def exhaustiveSearch(tsp: TSP.TSPLibInstance): ArrayForm =
//      (0 until tsp.numCities).toArray.permutations.map { a => new ArrayForm(a:_*) 
//        }.minBy { perm => TSP.tourLength(perm,tsp.getDistanceFn()) }
		
	  // val optimalTour = exhaustiveSearch(tsp)
	  // val optimalTourLength = TSP.tourLength(optimalTour,tsp.getDistanceFn())
    val OptimalTourLength = 27603 // for wi29.tsp		
	  
	  def relativeError(x: ArrayForm): Double = {
	    require( x.size() == tsp.numCities() )
	    (TSP.tourLength(x,tsp.getDistanceFn()) - OptimalTourLength)/OptimalTourLength
    }
    
    ///////////////////////////////		
		
    case class MyEnv(iter: Iter, maxIter: MaxIter, rng: RNG, tourLength: Evaluate[MyEnv,ArrayForm,Double])
    
		def fitnessImpl(problem: TSP.TSPLibInstance)(x: ArrayForm): Double =
		  TSP.tourLength(x,problem.getDistanceFn())
    
    case class TourLengthEval[Env](problem: TSP.TSPLibInstance) extends Evaluate[Env,ArrayForm,Double] {
      override def apply(x: ArrayForm): State[Env,Double] = State[Env,Double] { env =>
        (env,fitnessImpl(problem)(x))
      }
    }

  	val iterLens: Lens[MyEnv, Iter] = monocle.macros.GenLens[MyEnv] { _.iter }
  	  // Lens.lensu((x, newValue) => x.copy(iter = newValue), _.iter)
  	  
	  val maxIterLens: Lens[MyEnv, MaxIter] = 
	    monocle.macros.GenLens[MyEnv] { _.maxIter }
  	  // Lens.lensu((x, newValue) => x.copy(maxIter = newValue), _.maxIter)
  	  
	  val rngLens: Lens[MyEnv, RNG] = 
	    monocle.macros.GenLens[MyEnv] { _.rng }
  	  // Lens.lensu((x, newValue) => x.copy(rng = newValue), _.rng)
  	  
	  val tourLengthLens: Lens[MyEnv, Evaluate[MyEnv,ArrayForm,Double]] = 
	    monocle.macros.GenLens[MyEnv] { _.tourLength }
  	  // Lens.lensu((x, newValue) => x.copy(tourLength = newValue), _.tourLength)
  	  
	  ///////////////////////////////
	  
    val perturb: Perturb[MyEnv,ArrayForm] = 
        org.mitlware.hyperion3.immutable.perturb.permutation.EvaluateFull.RandomSwap(rngLens)
        
    val accept: Accept[MyEnv,ArrayForm] = AcceptImprovingOrEqual(isMinimizing=true, 
        scala.math.Ordering.Double, tourLengthLens)
    val isFinished: Condition[MyEnv,ArrayForm] = IsFinished.IterGreaterThanMaxIter(iterLens,maxIterLens)
		
	  val search = IteratedPerturbReturnLast(iterLens,perturb,accept,isFinished)

	  val initialEnv = MyEnv(Iter(0),MaxIter(maxIter),KnuthLCG64(seed), TourLengthEval(tsp) )
	  
	  val initialSol = TSPHeuristics.bestNearestNeighbour(tsp)
	  
    val startTime = System.currentTimeMillis()
	  val (finalEnv,solution) = search( initialSol ).run( initialEnv ).value
    val endTime = System.currentTimeMillis()
    Diag.println( s"elapsed: ${(endTime - startTime)/1000.0}" )
	  
	  Diag.println( relativeError(solution) )
	  Diag.println( fitnessImpl(tsp)(solution) )
	  
    val Threshold = 0.1
    val re = relativeError(solution)
    assertTrue( s"expected value <= $Threshold, found $re", re <= Threshold )
	}
}

// End ///////////////////////////////////////////////////////////////
