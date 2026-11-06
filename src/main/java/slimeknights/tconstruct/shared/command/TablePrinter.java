package slimeknights.tconstruct.shared.command;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Simple table builder used by the report subcommands.
 * <p>
 * TODO(fabric): this replaces Forge's {@code net.minecraftforge.common.util.TablePrinter}, which has no Mantle or Fabric equivalent.
 */
public class TablePrinter<T> {
  private final List<String> headers = new ArrayList<>();
  private final List<Function<T, String>> getters = new ArrayList<>();
  private final List<T> rows = new ArrayList<>();

  /** Adds a column to the table */
  public TablePrinter<T> header(String name, Function<T, String> getter) {
    headers.add(name);
    getters.add(getter);
    return this;
  }

  /** Adds a row to the table */
  public TablePrinter<T> add(T row) {
    rows.add(row);
    return this;
  }

  /** Adds multiple rows to the table */
  public TablePrinter<T> add(Iterable<? extends T> rows) {
    for (T row : rows) {
      add(row);
    }
    return this;
  }

  /** Builds the table into the given builder */
  public void build(StringBuilder builder) {
    builder.append('\n');
    int columns = headers.size();
    int[] widths = new int[columns];
    for (int i = 0; i < columns; i++) {
      widths[i] = headers.get(i).length();
    }
    List<String[]> values = new ArrayList<>(rows.size());
    for (T row : rows) {
      String[] cells = new String[columns];
      for (int i = 0; i < columns; i++) {
        String value = getters.get(i).apply(row);
        cells[i] = value == null ? "" : value;
        widths[i] = Math.max(widths[i], cells[i].length());
      }
      values.add(cells);
    }
    appendRow(builder, headers.toArray(new String[0]), widths);
    builder.append('\n');
    for (String[] cells : values) {
      appendRow(builder, cells, widths);
      builder.append('\n');
    }
  }

  /** Appends a single row with padded columns */
  private static void appendRow(StringBuilder builder, String[] cells, int[] widths) {
    for (int i = 0; i < cells.length; i++) {
      if (i > 0) {
        builder.append(" | ");
      }
      builder.append(cells[i]);
      for (int pad = cells[i].length(); pad < widths[i]; pad++) {
        builder.append(' ');
      }
    }
  }
}
