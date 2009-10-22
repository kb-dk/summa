package dk.statsbiblioteket.summa.common.shell;

import java.util.*;
import java.io.IOException;

/**
 * A helper class to print tabular data to an {@code Appendable}
 */
public class Layout {

    private List<String> columns;
    private boolean printHeaders;
    private Model model;
    private String delimiter;

    public static class Model implements Iterable<Map<String,String>> {

        private List<Map<String,String>> rows;
        private Map<String,Integer> columnWidths;

        public Model () {
            rows = new ArrayList<Map<String,String>>();
            columnWidths = new HashMap<String,Integer>();
        }

        public Map<String,String> appendRow(String... data) {
            if (data.length % 2 != 0) {
                throw new IllegalArgumentException("Uneven number of args "
                                                   + data.length);
            }

            Map<String,String> row = new HashMap<String,String>();

            for (int i = 0; i < data.length; i += 2) {
                row.put(data[i], data[i+1]);

                int oldWidth = columnWidths.containsKey(data[i]) ?
                                                  columnWidths.get(data[i]) : 0;
                int valueLength = data[i+1] != null ? data[i+1].length() : 0;
                columnWidths.put(data[i], Math.max(oldWidth, valueLength));
            }

            rows.add(row);
            return row;
        }

        public Map<String,String> appendData(String... data) {
            if (data.length % 2 != 0) {
                throw new IllegalArgumentException("Uneven number of args "
                                                   + data.length);
            }

            if (rows.size() == 0) {
                appendRow();
            }

            Map<String,String> row = rows.get(rows.size() - 1);
            for (int i = 0; i < data.length; i += 2) {
                row.put(data[i], data[i+1]);

                int oldWidth = columnWidths.containsKey(data[i]) ?
                                                  columnWidths.get(data[i]) : 0;
                columnWidths.put(data[i],
                                 Math.max(oldWidth, data[i+1].length()));
            }

            return row;
        }

        public Iterator<Map<String, String>> iterator() {
            return rows.iterator();
        }

        public int columnWidth(String col) {
            return columnWidths.containsKey(col) ? columnWidths.get(col) : 0;
        }
    }

    public Layout(List<String> columns) {
        this.columns = new ArrayList<String>(columns);
        printHeaders = true;
        model = new Model();
        delimiter = " ";
    }

    public Layout(String... columns) {
        this(Arrays.asList(columns));
    }

    public void setPrintHeaders(boolean printHeaders) {
        this.printHeaders = printHeaders;
    }

    public void setDelimiter (String delim) {
        this.delimiter = delim;
    }

    public void setColumns(String... columns) {
        setColumns(Arrays.asList(columns));
    }

    public void setColumns(List<String> columns) {
        this.columns = new ArrayList<String>(columns);
    }

    public void appendColumns(String... cols) {
        for (String col : cols) {
            columns.add(col);
        }
    }

    public Model getModel() {
        return model;
    }

    public void appendRow(String... data) {
        model.appendRow(data);
    }

    public void appendData(String... data) {
        model.appendData(data);
    }

    public Appendable print(Appendable buf) throws IOException {

        if (printHeaders) {
            for (String col : columns) {
                pad(buf, col, col);
                buf.append(delimiter);
            }
            buf.append("\n");
        }

        for (Map<String,String> row : model) {
            for (String col : columns) {
                String data = row.get(col);
                pad(buf, col, data);
                buf.append(delimiter);
            }
            buf.append("\n");
        }

        return buf;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        try {
            print(buf);
        } catch (IOException e) {
            buf.append("Error: ");
            buf.append(e.getMessage());
        }

        return buf.toString();
    }

    private int getColumnWidth(String col) {
        if (printHeaders) {
            return Math.max(model.columnWidth(col), col.length());
        }
        return model.columnWidth(col);
    }

    private Appendable pad(Appendable buf, String column, String data)
                                                            throws IOException {
        data = data != null ? data : "null";

        int num = getColumnWidth(column) - data.length();

        if (num <= 0) {
            return buf.append(data);
        }

        buf.append(data);
        for (int i = 0; i < num; i++) {
            buf.append(" ");
        }

        return buf;
    }
}

