import java.util.Vector;

/**
  * This is a basic class with some learning tools: statistical learning, learning automata (LA) and Q-Learning (QL)
  *
  * @author  Juan C. Burguillo Rial
  * @version 2.0
  */
  class ReinforcementLearning {
  final double dDecFactorLR = 0.99;			// Value that will decrement the learning rate in each generation
  private double dEpsilon = 0.4;		//0.3		// Used to avoid selecting always the best action
  final double dMINEpsilon = 0.0;			// Minimum value for epsilon
  final double dDecFactorEpsilon = 0.85;		// Value that will decrement epsilon in each generation
  final double dMINLearnRate = 0.1;			// We keep learning, after convergence, during 5% of times
  final double dGamma = 0.6;	//0.68				// Discount factor
  private double dLearnRate = 0.7;		//0.38	// Initial learning rate
  private boolean bAllActions = false;				// At the beginning we did not try all actions
  private int iNewAction;					// This is the new action to be played
  private int iNumActions = 2; 					// For C or D for instance
  private int iLastAction;					// The last action that has been played by this player
  private int iLastState = 0;					// The last state that has been played by this player
  private int iPresentState;				// The present state of the player
  private int[] iNumTimesAction = new int [iNumActions];	// Number of times an action has been played
  private double[] dPayoffAction = new double [iNumActions];	// Accumulated payoff obtained by the different actions
  private StateAction oPresentStateAction;
  private StateAction oLastStateAction = null;			// Contains the present state we are and the actions that are available
  private Vector<StateAction> oVStateActions = new Vector<>();					// A vector containing strings with the possible States and Actions available at each one
  private double dLastFunEval;
  
  /**
    * This method is used to select the next action (Schaerf) considering a statistical
    * criterium for the action that provided more benefits in the last iSizeBufferStat attempts.
    *
    */
  public int iGetNewActionStats () {
    double dAux, dAuxTot;
    double dProbAction[] = new double [iNumActions];
    double[] dAvgPayoffAction = new double [iNumActions];
  
    // Checking that I have played all actions before
    if (!bAllActions) {
      bAllActions = true;
      for (int i=0; i<iNumActions; i++)
        if (iNumTimesAction[i] == 0) {
          bAllActions = false;
          break;
          }
      }
    else {                // If all actions have been tested, the probabilities are adjusted
      dAuxTot = 0;
      for (int i=0; i<iNumActions; i++) {							// Calculating average incomes
        dAvgPayoffAction[i] = dPayoffAction[i] / (double) iNumTimesAction[i];		// Avg. value
        dAuxTot += dAvgPayoffAction[i];							// Adding the individual results
        }
  
      for (int i=0; i<iNumActions; i++)
        dProbAction[i] = dAvgPayoffAction[i] / dAuxTot;                      		// Calculating probs.
  
      }	// if (bAllActions)
  
  
    dAuxTot = 0;
    dAux = Math.random();
    for (int i=0; i<iNumActions; i++) {
      dAuxTot += dProbAction[i];
      if (dAux <= dAuxTot) {
          iNewAction = i;
          break;
        }
      }
      return iNewAction;
    }


  
  
  
  

  
  /**
   * This method uses Learning Automata (LA) to select a new action depending on the
   * past experiences. The algorithm works as: store, adjust and generate a new action.
   *	@param sState contains the present state
   *	@param iNActions contains the number of actions that can be applied in this state
   *	@param dFunEval is the new value of the function to evaluate (depends on the game).
   */
  public int iGetNewActionAutomata (String sState, int iNActions, double dFunEval) {
    boolean bFound;
    StateAction oStateProbs;
  
    bFound = false;							// Searching if we already have the state
    for (int i=0; i<oVStateActions.size(); i++) {
      oStateProbs = (StateAction) oVStateActions.elementAt(i);
      if (oStateProbs.sState.equals (sState)) {
        oPresentStateAction = oStateProbs;
        iPresentState = i;
        bFound = true;
        break;
      }
    }
     // If we didn't find it, then we add it
    if (!bFound) {
      oPresentStateAction = new StateAction (sState, iNActions, true);
      oVStateActions.add (oPresentStateAction);
    }
  


    if (oLastStateAction != null) {              				// Adjusting Probabilities
      if (dFunEval - dLastFunEval > 0)					// If reward grows and the previous action was allowed --> reinforce last action
        for (int i=0; i<iNActions; i++)
          if (i == iLastAction)
            oLastStateAction.dValAction[i] += dLearnRate * (1.0 - oLastStateAction.dValAction[i]);	// Reinforce the last action
          else
            oLastStateAction.dValAction[i] *= (1.0 - dLearnRate);		// The rest are weakened
            oVStateActions.setElementAt(oLastStateAction, iLastState);
    }
    
    double dValAcc = 0;							// Generating the new action based on probabilities
    double dValRandom = Math.random();
    for (int i=0; i<iNActions; i++) {
      dValAcc += oPresentStateAction.dValAction[i];
      if (dValRandom < dValAcc) {
        iNewAction = i;
        break;
      }
    }
    iLastState = iPresentState;
    oLastStateAction = oPresentStateAction;			// Updating values for the next time
    dLastFunEval = dFunEval;
    dLearnRate *= dDecFactorLR;					        // Reducing the learning rate
    if (dLearnRate < dMINLearnRate) dLearnRate = dMINLearnRate;
    return iNewAction;
  }
  
  
  
  
  
  
  
  
  /**
    * This method is used to implement Q-Learning:
    *  1. I start with the last action a, the previous state s and find the actual state s'
    *  2. Select the new action with Qmax{a'}
    *  3. Adjust:   Q(s,a) = Q(s,a) + dLearnRateLR [R + dGamma . Qmax{a'}(s',a') - Q(s,a)]
    *  4. Select the new action by a epsilon-greedy methodology
    *
    *	@param sState contains the present state
    *	@param iNActions contains the number of actions that can be applied in this state
    *	@param dFunEval is the new value of the function to evaluate (depends on the game). This value 
    * 	       minus last value also determines the reward to be used.
    */
  
  public int iGetNewActionQLearning (String sState, int iNActions, double dFunEval) {
    boolean bFound;
    int iBest=-1, iNumBest=1;
    double dR, dQmax;
    StateAction oStateAction;
   
    bFound = false;							// Searching if we already have the state
    
    for (int i=0; i<oVStateActions.size(); i++) {
      oStateAction = (StateAction) oVStateActions.elementAt(i);
      if (oStateAction.sState.equals(sState)) {
        oPresentStateAction = oStateAction;
        iPresentState = i;
        bFound = true;
        break;
      }
    }
                                                                          // If we didn't find it, then we add it
    if (!bFound) {
      oPresentStateAction = new StateAction(sState, iNActions);
      oVStateActions.add(oPresentStateAction);
      iPresentState = oVStateActions.size()-1;
    }
  
    dQmax = 0;
    for (int i=0; i<iNActions; i++) {					// Determining the action to get Qmax{a'}
      if (oPresentStateAction.dValAction[i] > dQmax) {
        iBest = i;
        iNumBest = 1;							// Reseting the number of best actions
        dQmax = oPresentStateAction.dValAction[i];
      }
      else if ( (oPresentStateAction.dValAction[i] == dQmax) && (dQmax > 0) ) {	// If there is another one equal we must select one of them randomly
        iNumBest++;
        if (Math.random() < 1.0 / (double) iNumBest) {				// Choose randomly with reducing probabilities
          iBest = i;
          dQmax = oPresentStateAction.dValAction[i]; 
        }
      }
    }
    // Adjusting Q(s,a)
    if (oLastStateAction != null) {
      //dR = dFunEval - dLastFunEval;					// Note that dR is also used as reward in the QL formulae						// If dFunEval is negative, then we are in a terminal state
      if (dFunEval >= 0)			// If reward grows and the previous action was allowed --> reinforce the previous action considering present values
        oLastStateAction.dValAction[iLastAction] +=  dLearnRate * (dFunEval + dGamma * dQmax - oLastStateAction.dValAction[iLastAction]);

      
      else {
        oLastStateAction.dValAction[iLastAction] *= (1 + dFunEval/20);		// The rest are weakened
        
      }
      oVStateActions.setElementAt(oLastStateAction, iLastState);
    }
    if ( (iBest > -1) && (Math.random() > dEpsilon) ) 			// Using the e-greedy policy to select the best action or any of the rest
      iNewAction = iBest;
    else do {
      iNewAction = (int) (Math.random() * (double) iNumActions);
    } while (iNewAction == iBest);
    
    	// Updating the values for the next time
    
    oLastStateAction = oPresentStateAction;				// Updating values for the next time
    iLastState = iPresentState;
    dLastFunEval = dFunEval;
    dLearnRate *= dDecFactorLR;						// Reducing the learning rate
    if (dLearnRate < dMINLearnRate) dLearnRate = dMINLearnRate;
    if (dEpsilon > dMINEpsilon) dEpsilon *= dDecFactorEpsilon;		// Reducing the epsilon
    return iNewAction;
  }
  

  }  // from class LearningTools
  
  

  
  
  