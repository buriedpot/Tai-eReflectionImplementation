package pascal.taie.analysis.pta.plugin.mock;

import java.util.List;
import java.util.Objects;

/**
 * @author buriedpot
 * JMethod which contains unknown elements
 * @date 2023/01/14
 */

public record UJMethod(String declaringClass, String returnType, String methodName, List<String> parameters) {
    public static final String UNKNOWN_STRING = null;
    public static final List<String> UNKNOWN_LIST = null;

    public boolean isReturnTypeUnknown() {
        return returnType == null;
    }
    public boolean isParametersUnknown() {
        return parameters == null;
    }

    public boolean isDeclaringClassUnknown() {
        return declaringClass == null;
    }
    public boolean isMethodNameUnknown() {
        return methodName == null;
    }

    public boolean isSubsignatureUnknown() {
        // TODO 和论文所表述的&&逻辑不同。必须得并，不然按照论文逻辑，有mthdName它也不会主动推测ptp
        // 暂时还是调整为and吧
        return Objects.equals(methodName, UNKNOWN_STRING) && Objects.equals(parameters, UNKNOWN_LIST);
    }

    // 是否除了return Type以外已经全部推测了出来
    public boolean isKnown() {
        return declaringClass != null && methodName != null && parameters != null;
    }

}
