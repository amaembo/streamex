package one.util.functionex.utils;

public class Utils {

    public static String throwableAction(String value) throws CustomException {
        if(value.contains("error"))
            throw new CustomException(value);
        return value;
    }
    public static String throwableAction(int value) throws CustomException {
        if(value < 0)
            throw new CustomException(String.valueOf(value));
        return String.valueOf(value);
    }
    public static int checkNonNegative(int value) throws CustomException {
        if(value < 0)
            throw new CustomException(String.valueOf(value));
        return value;
    }

}
