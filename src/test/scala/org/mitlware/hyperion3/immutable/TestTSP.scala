package org.mitlware.hyperion3.immutable

import cats.data.State
import monocle.Lens

import org.mitlware.hyperion3.immutable._
import org.mitlware.hyperion3.immutable.perturb._
import org.mitlware.hyperion3.immutable.accept._
import org.mitlware.hyperion3.immutable.isfinished._		

import org.mitlware.support.lang.BadFormatException
import org.mitlware.support.lang.Diag
import org.mitlware.support.lang.UnsupportedFormatException
import org.mitlware.support.math.Vec2

import org.mitlware.solution.permutation.ArrayForm
import org.mitlware.problem.tsp.TSP

import org.junit.Test

import java.io._
import java.nio.charset.Charset
import java.nio._
import java.nio.file._
import java.util._

import org.junit.Assert._ 

//////////////////////////////////////////////////////////////////////

class TestTSP {

	@Test
	def testIteratedPerturbation: Unit = {
		
		def exhaustiveSearch(tsp: TSP.TSPLibInstance): ArrayForm =
      (0 until tsp.numCities).toArray.permutations.map { a => new ArrayForm(a:_*) 
        }.minBy { perm => TSP.tourLength(perm,tsp.getDistanceFn()) }      
	  
		val path = System.getProperty( "user.dir" ) + "/resources/" + "unitTest.tsp";		
		val is = new FileInputStream( path );
		val tsp = new TSP.TSPLibInstance( is );
	  val optimalTour = exhaustiveSearch(tsp)
	  val optimalTourLength = TSP.tourLength(optimalTour,tsp.getDistanceFn())
	  
    ///////////////////////////////		
		
    case class MyEnv(iter: Iter, maxIter: MaxIter, rng: RNG, tourLength: Evaluate[MyEnv,ArrayForm,Double])
    
    case class TourLength[Env](problem: TSP.TSPLibInstance) extends Evaluate[Env,ArrayForm,Double] {
      override def apply(x: ArrayForm): State[Env,Double] = State[Env,Double] { env =>
        (env,TSP.tourLength(x,problem.getDistanceFn()))
      }
    }

  	val iterLens: Lens[MyEnv, Iter] = monocle.macros.GenLens[MyEnv] { _.iter }
	  val maxIterLens: Lens[MyEnv, MaxIter] = monocle.macros.GenLens[MyEnv] { _.maxIter }
	  val rngLens: Lens[MyEnv, RNG] = monocle.macros.GenLens[MyEnv] { _.rng }	  
	  val tourLengthLens: Lens[MyEnv, Evaluate[MyEnv,ArrayForm,Double]] = monocle.macros.GenLens[MyEnv] { _.tourLength }
    
    val perturb: Perturb[MyEnv,ArrayForm] = Permutation.RandomSwap(rngLens) 
    val accept: Accept[MyEnv,ArrayForm] = AcceptImprovingOrEqual(isMinimizing=true, tourLengthLens)
    val isFinished: Condition[MyEnv,ArrayForm] = IterGreaterThanMaxIter(iterLens,maxIterLens)
		
	  val search = IteratedPerturbation(iterLens,perturb,accept,isFinished)
	  val initialEnv = MyEnv(Iter(0),MaxIter(100),RNG(0xDEADBEEF), TourLength(tsp) )
	  val (finalEnv,solution) = search( new ArrayForm( tsp.numCities() ) ).run( initialEnv ).value
	  assertEquals( optimalTourLength, TSP.tourLength(solution,tsp.getDistanceFn()), 0.0 )
	}
}

// End ///////////////////////////////////////////////////////////////
