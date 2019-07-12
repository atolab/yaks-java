package is.yaks;

import java.util.Properties;

/**
 * Interface to be implemted by evaluation functions (see {@link Workspace#registerEval(Path, Eval)})
 */
public interface Eval {

    /**
     * The callback operation called each time the {@link Workspace#eval(Selecror)} operation is called
     * for a Selector matching the Path this Eval is registered with.
     * 
     * @param path the Path with which the Eval has been registered with (in case the same Eval is registered with several Paths).
     * @param props the Properties specified in the Selector used in eval operation. 
     */
    public Value callback(Path path, Properties props);

}
