import java.io.Serializable;

/**
    * This is the basic class to store Q values (or probabilities) and actions for a certain state
    *
    * @author  Juan C. Burguillo Rial
    * @version 2.0
    */
    public class StateAction implements Serializable
    {
    String sState;
    double[] dValAction;
    
    StateAction (String sAuxState, int iNActions) {
      sState = sAuxState;
      dValAction = new double[iNActions];
      }
    
    StateAction (String sAuxState, int iNActions, boolean bLA) {
      this (sAuxState, iNActions);
      if (bLA) for (int i=0; i<iNActions; i++)	// This constructor is used for LA and sets up initial probabilities
        dValAction[i] = 1.0 / iNActions;
      }
    
    
    public String sGetState() {
      return sState;
    }
    
    public double dGetQAction (int i) {
      return dValAction[i];
    }
    }