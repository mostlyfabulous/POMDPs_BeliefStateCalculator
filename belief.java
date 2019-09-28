import java.util.Arrays;
import java.util.List;
import static java.util.Arrays.*;

/**
 * Created by Andrew on 2019-09-25.
 */
public class belief {
    static final int nonTerminalStates = 9;
    static Boolean startGiven = false;
    static int startCol;
    static int startRow;
    // zero indexed values
    static final int maxCol = 4;
    static final int maxRow = 3;
    // moving probabilities
    static final double pleft = 1d/10;
    static final double pright = 1d/10;
    static final double pfwd = 8d/10;
    // index zero for maxWalls is equivalent to "end" being observed
    static final int maxWalls = 3;
    // observation model probabilities
    static double[][][] obsModl = new double[maxCol][maxRow][maxWalls];
    // belief states
    static double[][] currBelief = new double[maxCol][maxRow];
    static double[][] nextBelief = new double[maxCol][maxRow];
    // normalizing factor
    static double norm = 0;
    static String[] actions;
    static int[] evidence;

    public static void setUp() {
        for (int col = 0; col < maxCol; col++) {
            for (int row = 0; row < maxRow; row++) {
                if (startGiven) currBelief[col][row] = 0;
                else currBelief[col][row] = 1d/nonTerminalStates;
                // Non-terminal in third column
                if (col == 2) {
                    obsModl[col][row][0] = 0.0;
                    obsModl[col][row][1] = 9d/10;
                    obsModl[col][row][2] = 1d/10;
                }
                // All other non-terminal
                else {
                    obsModl[col][row][0] = 0.0;
                    obsModl[col][row][1] = 1d/10;
                    obsModl[col][row][2] = 9d/10;
                }
            }
        }
        // set starting belief and adjust for zero indexing
        if (startGiven) currBelief[startCol-1][startRow-1] = 1;
        // set belief of (2,2) = [1][1] to 0 (represents a column)
        currBelief[1][1] = 0;
        // set terminal states to probability 0
        currBelief[3][2] = 0;
        currBelief[3][1] = 0;
        // set (2,2) = [1,1] to 0
        obsModl[1][1][0] = 0;
        obsModl[1][1][1] = 0;
        obsModl[1][1][2] = 0;
        // set terminal states to probability 0 for wall observations
        obsModl[3][2][0] = 1d;
        obsModl[3][2][1] = 0;
        obsModl[3][2][2] = 0;

        obsModl[3][1][0] = 1d;
        obsModl[3][1][1] = 0;
        obsModl[3][1][2] = 0;
//        System.out.println("Obs model");
//        String[] a = deepToString(obsModl).split("]],");
//        for (String s : a) {System.out.println(s);}
    }

    public static int parseArgs(String[] args) {
        String[] as = args[0].split(",");
        actions = new String[as.length];
        int i = 0;
        for (String a: as
             ) {
            actions[i] = a.trim();
            i++;
        }
        System.out.print("Actions: " + Arrays.toString(actions) + " ");
        List<String> es = asList(args[1].trim().split(","));

        evidence = new int[es.size()];
        try {
            for (int j = 0; j < es.size(); j++) {
                if (es.get(j).contains("end")) evidence[j] = 0;
                else evidence[j] = Integer.parseInt(es.get(j));
            }
        } catch (Exception e) {
            System.out.println("Evidence given contained non-numerical characters");
        }
        System.out.println("Evidence: " + Arrays.toString(evidence));

        // determine starting state
        if (args.length > 2) {
            startGiven = true;
            String[] pos = args[2].split(",");
            startCol = Integer.parseInt(pos[0]);
            startRow = Integer.parseInt(pos[1]);
            System.out.println("Starting state: S0=("+ (startCol) + "," + (startRow)+")\n");
        }
        return 1;
    }

    public static double updateBelief(String action, int evidence, int col, int row) {
        nextBelief[col][row] = obsModl[col][row][evidence] * move(col, row, action, evidence);
        return nextBelief[col][row];
    }

    public static int wallCheck(int col, int row) {
        // if no wall collision return 1 and add/subtract it to/from col or row
        if (col >= maxCol || col < 0) return 0;
        else if (row >= maxRow || row < 0) return 0;
        // blocked of grid square at (2,2) = [1][1]
        else if (row == 1 && col == 1) return 0;
        else return 1;
    }

    public static double move(int col, int row, String action, int e) {
        // calculate b'(s) by checking neighbouring grid spaces and probabilities
        if (wallCheck(col, row) == 0) return 0;
        int colRt = col + wallCheck(col+1, row); // right
        int colLt = col - wallCheck(col-1, row); // left
        int rowUp = row + wallCheck(col, row+1); // up
        int rowDn = row - wallCheck(col, row-1); // down
        int bouncedUp = (wallCheck(col, row+1) == 0 ) ? 1 : 0;
        int bouncedDn = (wallCheck(col, row-1) == 0 ) ? 1 : 0;
        int bouncedLt = (wallCheck(col-1, row) == 0 ) ? 1 : 0;
        int bouncedRt = (wallCheck(col+1, row) == 0 ) ? 1 : 0;
        double calc = 0;
        switch (action) {
            case "up":
//                System.out.format("%.1f * %.3f * %x + %.1f * %.3f * %x + %.1f * %.3f + %.1f * %.3f \n",
//                        pfwd, currBelief[col][rowUp], bouncedUp, pfwd, currBelief[col][rowDn],
//                        wallCheck(col, row-1), pright, currBelief[colLt][row], pleft, currBelief[colRt][row]);
                calc =
                        pfwd*currBelief[col][rowUp] * bouncedUp +// bounced from wall going up
                        pfwd*currBelief[col][rowDn] * wallCheck(col, row-1) +// cannot come from below boundary
                        pright*currBelief[colLt][row] + pleft*currBelief[colRt][row]; // moved sideways
                return calc;
            case "down":
                calc =
                        pfwd*currBelief[col][rowDn] * bouncedDn +// bounced from wall going down
                        pfwd*currBelief[col][rowUp] * wallCheck(col, row+1) +//cannot come from above boundary
                        pright*currBelief[colLt][row] + pleft*currBelief[colRt][row]; // moved sideways
                return calc;
            case "left":
                calc =
                        pfwd*currBelief[colLt][row] * bouncedLt + // bounced from wall going left
                        pfwd*currBelief[colRt][row] * wallCheck(col+1, row) + // cannot come from right of boundary
                        pleft*currBelief[col][rowUp] + pright*currBelief[col][rowDn]; // moved sideways
                return calc;
            case "right":
//                System.out.format("(%x,%x): ", col, row);
//                System.out.format("%.1f * %.3f * %x + %.1f * %.3f * %x + %.1f * %.3f + %.1f * %.3f \n",
//                        pfwd, currBelief[colRt][row], bouncedRt, pfwd, currBelief[colLt][row],
//                        wallCheck(col-1, row), pright, currBelief[col][rowUp], pleft, currBelief[col][rowDn]);
                calc =
                        pfwd*currBelief[colRt][row] * bouncedRt + // bounced from wall going right
                        pfwd*currBelief[colLt][row] * wallCheck(col-1, row) +  // cannot come from left of boundary
                        pright*currBelief[col][rowUp] + pleft*currBelief[col][rowDn]; // moved sideways
                return calc;
            default:
                System.out.println("Invalid action was passed");
                break;
        }
        return 0;
    }

    public static void main(String[] args) {
        String[] seq1 = {"up, up, up", "2,2,2"};
        String[] seq2 = {"up, up, up", "1,1,1"};
        String[] seq3 = {"right, right, up", "1,1,end", "2,3"};
        String[] seq4 = {"up, right, right, right", "2,2,1,1", "1,1"};
        parseArgs(seq3);
        setUp();
        System.out.println("State: 0" );
        for (int row = maxRow-1; row > -1; row--) {
            for (int col = 0; col < maxCol; col++) {
                System.out.format("%.5f, ", currBelief[col][row]);
            }
            System.out.println();
        }
        System.out.println();

        for (int state = 0; state < actions.length; state++) {
            System.out.println("State: " + (state+1));
            for (int row = maxRow-1; row > -1; row--) {
                for (int col = 0; col < maxCol; col++) {
                    norm += updateBelief(actions[state], evidence[state], col, row);
                }
//                System.out.println();
            }
            // normalize probabilities for entire grid
            for (int row = maxRow-1; row > -1; row--) {
                for (int col = 0; col < maxCol; col++) {
                    nextBelief[col][row] = nextBelief[col][row]/norm;
                    System.out.format("%.5f, ", nextBelief[col][row]);
                }
                System.out.println();
            }
            System.out.println();
            // copy values in nextBelief to currBelief for next iteration
            currBelief = new double[maxCol][maxRow];
            for (int i = 0; i < nextBelief.length; i++) {
                currBelief[i] = Arrays.copyOf(nextBelief[i], nextBelief[i].length);
            }
            norm = 0;
        }

    }

}
