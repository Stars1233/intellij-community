import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Test {
  public static <Om> List<Om> sort(Comparator comp, Stream<Om> stream) {
    return stream.sorted(comp).<error descr="Incompatible types. Found: 'java.lang.Object', required: 'java.util.List<Om>'">collect</error>(Collectors.toList());
  }

  //accept unbounded wildcards
  List<String> get(List<?> lists) {
    return null;
  }

  void foo(List l) {
    String p = get(l).get(0);
  }
}
