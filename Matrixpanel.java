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
        setBorder(BorderFactory.createEmptyBorder(6, 8, 4, 8));
 
        add(buildSizeRow());
        gridsRow = buildGridsRow();
        add(gridsRow);
        add(Box.createVerticalStrut(4));
        add(buildOpsRow());
        add(Box.createVerticalStrut(4));
 
        resultLabel = new JLabel("Enter values, then pick an operation.");
        resultLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
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
        twoByTwoBtn = smallButton("2\u00D72");
        threeByThreeBtn = smallButton("3\u00D73");
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
    }
 
    private JPanel buildGridsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
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
                f.setFont(new Font("Monospaced", Font.PLAIN, 11));
                f.setBackground(Theme.KEY_BG);
                f.setForeground(Theme.DIGIT_FG);
                f.setCaretColor(Theme.GOLD_BRIGHT);
                f.setBorder(BorderFactory.createLineBorder(Theme.PILL_BORDER, 1));
                f.setPreferredSize(new Dimension(30, 22));
                cells[r][c] = f;
            }
        }
        return cells;
    }
 
    private JPanel wrapGrid(JTextField[][] cells) {
        JPanel grid = new JPanel(new GridLayout(size, size, 2, 2));
        grid.setBackground(Theme.DISPLAY_BG);
        for (JTextField[] row : cells) for (JTextField f : row) grid.add(f);
        return grid;
    }
 
    private JPanel buildOpsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(Theme.DISPLAY_BG);
        row.add(opButton("A+B", e -> compute('+')));
        row.add(opButton("A\u00D7B", e -> compute('\u00D7')));
        row.add(opButton("det(A)", e -> computeDet(cellsA, 'A')));
        row.add(opButton("det(B)", e -> computeDet(cellsB, 'B')));
        return row;
    }
}