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
}