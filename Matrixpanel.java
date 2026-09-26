import javax.swing.*;
import java.awt.*;

public class MatrixPanel extends JPanel {
    private int size = 2; // 2 or 3
    private JTextField[][] cellsA, cellsB;
    private JPanel gridsRow;
    private RoundedButton twoByTwoBtn, threeByThreeBtn;
    private JLabel resultLabel;
 
    public MatrixPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Theme.DISPLAY_BG);
        // INCREASED: Outer padding for a less crowded UI
        setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
 
        add(buildSizeRow());
        add(Box.createVerticalStrut(10)); // INCREASED: Space between size selection and grids
        gridsRow = buildGridsRow();
        add(gridsRow);
        add(Box.createVerticalStrut(10)); // INCREASED: Space between grids and buttons
        add(buildOpsRow());
        add(Box.createVerticalStrut(12)); // INCREASED: Space before result text
 
        resultLabel = new JLabel("Enter values, then pick an operation.");
        // INCREASED: Clearer typography for output display
        resultLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultLabel.setForeground(Theme.GOLD_BRIGHT);
        resultLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(resultLabel);
    }
    
    public void focusFirstCell() {
        if (cellsA != null && cellsA.length > 0) cellsA[0][0].requestFocusInWindow();
    }
    
    private JPanel buildSizeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(Theme.DISPLAY_BG);
        twoByTwoBtn = smallButton("2×2");
        threeByThreeBtn = smallButton("3×3");
        twoByTwoBtn.setActive(true);
        twoByTwoBtn.addActionListener(e -> setSize(2));
        threeByThreeBtn.addActionListener(e -> setSize(3));
        row.add(twoByTwoBtn);
        row.add(threeByThreeBtn);
        return row;
    }
 
    private void setSize(int n) {
        size = n;
        twoByTwoBtn.setActive(n == 2);
        threeByThreeBtn.setActive(n == 3);
        int idx = getComponentZOrder(gridsRow);
        remove(gridsRow);
        gridsRow = buildGridsRow();
        add(gridsRow, idx);
        resultLabel.setText("Enter values, then pick an operation.");
        
        revalidate();
        repaint();
        resizeParentWindow(); // FIXED: Recalculates layout window frame sizes dynamically
    }
 
    private JPanel buildGridsRow() {
        // INCREASED: 20px gap between Matrix A and Matrix B
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        row.setBackground(Theme.DISPLAY_BG);
        cellsA = buildGrid();
        cellsB = buildGrid();
        row.add(wrapGrid(cellsA));
        row.add(wrapGrid(cellsB));
        return row;
    }
    
    private JTextField[][] buildGrid() {
        JTextField[][] cells = new JTextField[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                JTextField f = new JTextField("0");
                f.setHorizontalAlignment(JTextField.CENTER);
                // INCREASED: Slightly larger text field fonts
                f.setFont(new Font("Monospaced", Font.PLAIN, 12));
                f.setBackground(Theme.KEY_BG);
                f.setForeground(Theme.DIGIT_FG);
                f.setCaretColor(Theme.GOLD_BRIGHT);
                f.setBorder(BorderFactory.createLineBorder(Theme.PILL_BORDER, 1));
                // FIXED: Enlarged cells from (30x22) to (44x28) to stop values from clip cutting
                f.setPreferredSize(new Dimension(44, 28));
                cells[r][c] = f;
            }
        }
        return cells;
    }
 
    private JPanel wrapGrid(JTextField[][] cells) {
        // INCREASED: 4px padding gaps between matrix squares
        JPanel grid = new JPanel(new GridLayout(size, size, 4, 4));
        grid.setBackground(Theme.DISPLAY_BG);
        for (JTextField[] row : cells) for (JTextField f : row) grid.add(f);
        return grid;
    }
 
    private JPanel buildOpsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(Theme.DISPLAY_BG);
        row.add(opButton("A+B", e -> compute('+')));
        row.add(opButton("A×B", e -> compute('×')));
        row.add(opButton("det(A)", e -> computeDet(cellsA, 'A')));
        row.add(opButton("det(B)", e -> computeDet(cellsB, 'B')));
        return row;
    }
    
    private RoundedButton smallButton(String text) {
        RoundedButton b = new RoundedButton(text, 10, Theme.KEY_BG, Theme.GOLD, null);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        // INCREASED: Slightly taller and wider buttons
        b.setPreferredSize(new Dimension(55, 24));
        return b;
    }
 
    private RoundedButton opButton(String label, java.awt.event.ActionListener listener) {
        RoundedButton b = new RoundedButton(label, 10, Theme.KEY_BG, Theme.GOLD, null);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        // INCREASED: Comfort dimension boundaries for operations buttons
        b.setPreferredSize(new Dimension(68, 25));
        b.addActionListener(listener);
        return b;
    }
 
    private double[][] readMatrix(JTextField[][] cells) {
        double[][] m = new double[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                m[r][c] = Double.parseDouble(cells[r][c].getText().trim());
            }
        }
        return m;
    }
 
    private void compute(char op) {
        try {
            double[][] a = readMatrix(cellsA);
            double[][] b = readMatrix(cellsB);
            double[][] result = (op == '+') ? add(a, b) : multiply(a, b);
            resultLabel.setText("<html>" + formatMatrixHtml(result) + "</html>");
        } catch (NumberFormatException ex) {
            resultLabel.setText("Error: every cell needs a number.");
        }
        resizeParentWindow(); // FIXED: Expands popup frame safely to host the multiline matrix text
    }
    
    private void computeDet(JTextField[][] cells, char which) {
        try {
            double[][] m = readMatrix(cells);
            double d = determinant(m);
            resultLabel.setText("det(" + which + ") = " + CalcUtils.trimZero(d));
        } catch (NumberFormatException ex) {
            resultLabel.setText("Error: every cell needs a number.");
        }
        resizeParentWindow(); // FIXED: Adjusts window safely
    }
 
    private String formatMatrixHtml(double[][] m) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : m) {
            sb.append("[ ");
            for (int c = 0; c < row.length; c++) {
                sb.append(CalcUtils.trimZero(row[c]));
                if (c < row.length - 1) sb.append(", ");
            }
            sb.append(" ]<br>");
        }
        return sb.toString();
    }
    
    // NEW HELPER: Dynamically scales the outer modal frame when content updates
    private void resizeParentWindow() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.pack();
        }
    }
    
    private static double[][] add(double[][] a, double[][] b) {
        int n = a.length;
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) r[i][j] = a[i][j] + b[i][j];
        }
        return r;
    }
    
    private static double[][] multiply(double[][] a, double[][] b) {
        int n = a.length;
        double[][] r = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0;
                for (int k = 0; k < n; k++) sum += a[i][k] * b[k][j];
                r[i][j] = sum;
            }
        }
        return r;
    }
    
    private static double determinant(double[][] m) {
        int n = m.length;
        if (n == 1) return m[0][0];
        if (n == 2) return m[0][0] * m[1][1] - m[0][1] * m[1][0];
        double det = 0;
        for (int col = 0; col < n; col++) {
            double sign = (col % 2 == 0) ? 1 : -1;
            det += sign * m[0][col] * determinant(minor(m, 0, col));
        }
        return det;
    }
 
    private static double[][] minor(double[][] m, int skipRow, int skipCol) {
        int n = m.length;
        double[][] result = new double[n - 1][n - 1];
        int ri = 0;
        for (int r = 0; r < n; r++) {
            if (r == skipRow) continue;
            int ci = 0;
            for (int c = 0; c < n; c++) {
                if (c == skipCol) continue;
                result[ri][ci] = m[r][c];
                ci++;
            }
            ri++;
        }
        return result;
    }
}
