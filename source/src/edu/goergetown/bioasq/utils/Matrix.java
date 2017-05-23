package edu.goergetown.bioasq.utils;

/**
 * Created by Yektaie on 5/15/2017.
 */
public class Matrix {
    private double[][] data = null;

    public Matrix(int row, int column) {
        data = new double[row][];

        for (int i = 0; i < row; i++) {
            data[i] = new double[column];
        }
    }

    public void set(int row, int column, double value, boolean miror) {
        set(row, column, value);
        if (miror) {
            set(column, row, value);
        }
    }

    public void set(int row, int column, double value) {
        data[row][column] = value;
    }

    public double get(int row, int column) {
        return data[row][column];
    }

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return data[0].length;
    }

    public void print(int precision) {
        StringBuilder result = new StringBuilder();

        for (int row = 0; row < data.length; row++) {
            String del = "";
            for (int column = 0; column < data[row].length; column++) {
                result.append(del);
                result.append(String.format("%." + precision + "f", data[row][column]));
                del = "    ";
            }

            result.append("\n");
        }

        System.out.println(result.toString());
    }

    public double sumColumn(int column) {
        double result = 0;

        for (int row = 0; row < data.length; row++) {
            result += data[row][column];
        }

        return result;
    }

    public double sumRow(int row) {
        double result = 0;

        for (int column = 0; column < data[0].length; column++) {
            result += data[row][column];
        }

        return result;
    }

    public MatrixElement getMaximumElement(boolean mirrored) {
        MatrixElement result = new MatrixElement();
        result.value = -1;

        for (int row = 0; row < data.length; row++) {
            int count = mirrored ? row : data[row].length;
            for (int column = 0; column < count; column++) {
                if (data[row][column] > result.value) {
                    result.value = data[row][column];
                    result.row = row;
                    result.column = column;
                }
            }
        }

        return result;
    }
}
