import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 
public class Calculator extends JFrame {
     private final StringBuilder expr = new StringBuilder();      // COMP-mode expression
    private final StringBuilder funcExpr = new StringBuilder();  // GRAPH-mode f(X) expression
    private boolean isGraphMode = false;
    private boolean useDegrees = false; // reference defaults to RAD
    private boolean shiftMode = false;
    private boolean alphaMode = false;
    private double memory = 0, lastAnswer = 0;
    private boolean historyVisible = true;
    private final List<String> history = new ArrayList<>();
    /** Stored A-F variables (ALPHA+digit to recall, SHIFT+ALPHA+digit to store). Shared by
     *  reference with graphCanvas.variables so plotted functions see the latest values too. */
    private final Map<Character, Double> variables = new HashMap<>();
    private JTextField compDisplay;
    private GraphCanvas graphCanvas;
    private JPanel displayContainer;
    private CardLayout cardLayout;
    private JButton compTab, graphTab, shiftButton, alphaButton, degRadButton;
    private JButton xButton, ansButton, mPlusButton, mRecallButton;
    private JLabel angleValueLabel, calcModeValueLabel, historyCountLabel, historyToggleLabel;
    private JPanel historyListPanel;
    private JScrollPane historyScroll;
     public Calculator() {
        setTitle("Scientific Graphing Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(Theme.APP_BG);
 
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.APP_BG);
        wrapper.setBorder(new EmptyBorder(16, 16, 16, 16));
 
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Theme.APP_BG);
        card.setBorder(BorderFactory.createLineBorder(Theme.PILL_BORDER, 1));
 
        card.add(buildStatusBar());
        card.add(buildDisplayArea());
        card.add(buildInfoRow());
        card.add(buildHistorySection());
        card.add(Box.createVerticalStrut(6));
        card.add(buildTopPillRow());
        card.add(buildSciRow(new String[]{"sin", "cos", "tan", "ln", "log", "sqrt"}));
        card.add(buildSciRow(new String[]{"(", ")", "^", "pi", "e", "DEL", "\u00B1", "%"}));
        card.add(buildSciRow(new String[]{"cbrt", "x^2", "x^-1", "10^x", "X", "Ans", "M+", "MR"}));
        card.add(buildDigitGrid());
        card.add(buildExeBar());
        card.add(Box.createVerticalStrut(8));
 
        JLabel credit = new JLabel("Develped By Tamana Farzami", SwingConstants.CENTER);
        credit.setFont(new Font("Serif", Font.PLAIN, 12));
        credit.setForeground(Theme.TEXT_DIM);
        credit.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(credit);
        card.add(Box.createVerticalStrut(6));
 
        wrapper.add(card, BorderLayout.CENTER);
        add(wrapper);
        setMode(false);
        updateHistoryPanel();
        installKeyBindings();
        pack();
        setLocationRelativeTo(null);
    }

    //  Keyboard input: lets every key below drive the same onButton()/onExe() logic the
    //  on-screen buttons use, so typing works exactly like clicking - including ALPHA/SHIFT
    //  interactions, since digit key presses go through the real onButton() switch.
    private void installKeyBindings() {
        JComponent root = getRootPane();
        // WHEN_IN_FOCUSED_WINDOW: fires regardless of which button (if any) currently has
        // keyboard focus, as long as the calculator window itself is the active window.
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        // Digits: both top-row number keys and numpad, each bound to the same cmd string
        // used by the on-screen digit buttons ("0".."9") - this also means typing a digit
        // while ALPHA is armed recalls/stores a variable, exactly like clicking would.
        for (int d = 0; d <= 9; d++) {
            String digit = String.valueOf(d);
            bindKey(im, am, KeyStroke.getKeyStroke((char) ('0' + d)), "kbd.digit" + d, () -> simulateButton(digit));
            bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_NUMPAD0 + d, 0), "kbd.numpad" + d, () -> simulateButton(digit));
        }
        bindKey(im, am, KeyStroke.getKeyStroke('.'), "kbd.dot", () -> simulateButton("."));
        bindKey(im, am, KeyStroke.getKeyStroke('+'), "kbd.plus", () -> simulateButton("+"));
        bindKey(im, am, KeyStroke.getKeyStroke('-'), "kbd.minus", () -> simulateButton("-"));
        bindKey(im, am, KeyStroke.getKeyStroke('*'), "kbd.mult", () -> simulateButton("\u00D7"));
        bindKey(im, am, KeyStroke.getKeyStroke('/'), "kbd.div", () -> simulateButton("\u00F7"));
        bindKey(im, am, KeyStroke.getKeyStroke('^'), "kbd.pow", () -> simulateButton("^"));
        bindKey(im, am, KeyStroke.getKeyStroke('('), "kbd.lparen", () -> simulateButton("("));
        bindKey(im, am, KeyStroke.getKeyStroke(')'), "kbd.rparen", () -> simulateButton(")"));
        bindKey(im, am, KeyStroke.getKeyStroke('x'), "kbd.xLower", () -> simulateButton("X"));
        bindKey(im, am, KeyStroke.getKeyStroke('X'), "kbd.xUpper", () -> simulateButton("X"));

        // Functional keys
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "kbd.enter", this::onExe);
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "kbd.backspace", () -> simulateButton("DEL"));
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "kbd.delete", () -> simulateButton("DEL"));
        bindKey(im, am, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "kbd.escape", () -> simulateButton("AC"));
    }

    private void bindKey(InputMap im, ActionMap am, KeyStroke ks, String name, Runnable action) {
        im.put(ks, name);
        am.put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }
    private void simulateButton(String cmd) {
        onButton(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, cmd));
    }
     private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.APP_BG);
        bar.setBorder(new EmptyBorder(8, 10, 8, 10));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
 
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setBackground(Theme.APP_BG);
        compTab = flatTextButton("COMP");
        graphTab = flatTextButton("GRAPH");
        compTab.addActionListener(e -> setMode(false));
        graphTab.addActionListener(e -> setMode(true));
        left.add(compTab);
        left.add(graphTab);
 
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setBackground(Theme.APP_BG);
        angleValueLabel = smallLabel("RAD");
        JLabel deg = smallLabel("DEG");
        JLabel matrix = smallLabel("MATRIX");
        matrix.setForeground(Theme.TEXT_DIM);
        right.add(angleValueLabel);
        right.add(deg);
        right.add(matrix);
        right.add(decorativeIcon());
 
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
 
    private JButton flatTextButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setForeground(Theme.TEXT_DIM);
        b.setBackground(Theme.APP_BG);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        return b;
    }
 
    private JLabel smallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Theme.TEXT_DIM);
        return l;
    }
     private JComponent decorativeIcon() {
        // Purely cosmetic circular icon in the corner, matching the reference - not wired to any feature.
        JLabel icon = new JLabel("\u223F", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.PILL_BG);
                g2.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(Theme.GOLD);
                g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setForeground(Theme.GOLD);
        icon.setFont(new Font("SansSerif", Font.PLAIN, 12));
        icon.setPreferredSize(new Dimension(22, 22));
        return icon;
    }
 
    //  Display area: COMP text field or GRAPH canvas, swapped via CardLayout 
    private JPanel buildDisplayArea() {
        cardLayout = new CardLayout();
        displayContainer = new JPanel(cardLayout);
        displayContainer.setPreferredSize(new Dimension(420, 170));
        displayContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
 
        compDisplay = new JTextField();
        compDisplay.setFont(new Font("Serif", Font.BOLD, 24));
        compDisplay.setEditable(false);
        compDisplay.setHorizontalAlignment(JTextField.RIGHT);
        compDisplay.setBackground(Theme.DISPLAY_BG);
        compDisplay.setForeground(Theme.GOLD_BRIGHT);
        compDisplay.setCaretColor(Theme.GOLD_BRIGHT);
        compDisplay.setBorder(new EmptyBorder(10, 14, 10, 14));
        compDisplay.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        compDisplay.setToolTipText("Click to copy");
        compDisplay.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { copyDisplayToClipboard(); }
        });
 
        graphCanvas = new GraphCanvas();
        graphCanvas.useDegrees = useDegrees;
        graphCanvas.variables = variables;
 
        displayContainer.add(compDisplay, "COMP");
        displayContainer.add(graphCanvas, "GRAPH");
 
        JPanel holder = new JPanel(new BorderLayout());
        holder.setBackground(Theme.APP_BG);
        holder.add(displayContainer, BorderLayout.CENTER);
        return holder;
    }
     private JPanel buildInfoRow() {
        JPanel row = new JPanel(new GridLayout(1, 3));
        row.setBackground(Theme.APP_BG);
        row.setBorder(new EmptyBorder(10, 10, 10, 10));
 
        row.add(infoCell("ANGLE MODE", angleValueLabel = boldValueLabel("RAD")));
        row.add(infoCell("CALC MODE", calcModeValueLabel = boldValueLabel("COMP")));
 
        JPanel historyCell = new JPanel();
        historyCell.setLayout(new BoxLayout(historyCell, BoxLayout.Y_AXIS));
        historyCell.setBackground(Theme.APP_BG);
        JPanel captionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        captionRow.setBackground(Theme.APP_BG);
        JLabel caption = smallLabel("HISTORY PANEL");
        historyToggleLabel = new JLabel("HIDE");
        historyToggleLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        historyToggleLabel.setForeground(Theme.GOLD);
        historyToggleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        historyToggleLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { toggleHistory(); }
        });
        captionRow.add(caption);
        captionRow.add(historyToggleLabel);
        historyCountLabel = boldValueLabel("0 ITEMS");
        historyCell.add(captionRow);
        historyCell.add(historyCountLabel);
        row.add(historyCell);
 
        return row;
    }
     private JPanel infoCell(String caption, JLabel valueLabel) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBackground(Theme.APP_BG);
        cell.add(smallLabel(caption));
        cell.add(valueLabel);
        return cell;
    }
 
    private JLabel boldValueLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 14));
        l.setForeground(Theme.GOLD_BRIGHT);
        return l;
    }
    private JPanel buildHistorySection() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.APP_BG);
        wrap.setBorder(new EmptyBorder(0, 10, 10, 10));
 
        JLabel header = smallLabel("CALCULATION HISTORY");
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
 
        historyListPanel = new JPanel();
        historyListPanel.setLayout(new BoxLayout(historyListPanel, BoxLayout.Y_AXIS));
        historyListPanel.setBackground(Theme.HISTORY_BG);
 
        historyScroll = new JScrollPane(historyListPanel);
        historyScroll.setBorder(BorderFactory.createLineBorder(Theme.PILL_BORDER, 1));
        historyScroll.setPreferredSize(new Dimension(420, 70));
        historyScroll.getViewport().setBackground(Theme.HISTORY_BG);
        historyScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        historyScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
 
        wrap.add(header, BorderLayout.NORTH);
        wrap.add(historyScroll, BorderLayout.CENTER);
        return wrap;
    }
    private void toggleHistory() {
        historyVisible = !historyVisible;
        historyScroll.setVisible(historyVisible);
        historyToggleLabel.setText(historyVisible ? "HIDE" : "SHOW");
        historyScroll.getParent().revalidate();
        historyScroll.getParent().repaint();
    }
  private void updateHistoryPanel() {
        historyListPanel.removeAll();
        if (history.isEmpty()) {
            JLabel empty = new JLabel("No past calculations yet", SwingConstants.CENTER);
            empty.setFont(new Font("SansSerif", Font.ITALIC, 12));
            empty.setForeground(Theme.TEXT_DIM);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(14, 0, 14, 0));
            historyListPanel.add(empty);
        } else {
            for (int i = history.size() - 1; i >= 0; i--) {
        String text = history.get(i); // captured per-iteration, safe to use in the listener below
        JLabel entry = new JLabel(text);
        entry.setFont(new Font("SansSerif", Font.PLAIN, 12));
        entry.setForeground(Theme.DIGIT_FG);
        entry.setBorder(new EmptyBorder(4, 8, 4, 8));
        entry.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entry.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { loadHistoryEntry(text); }
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { entry.setForeground(Theme.GOLD_BRIGHT); }
            @Override public void mouseExited(java.awt.event.MouseEvent e) { entry.setForeground(Theme.DIGIT_FG); }
        });
        historyListPanel.add(entry);
    }
        }
        historyCountLabel.setText(history.size() + " ITEMS");
        historyListPanel.revalidate();
        historyListPanel.repaint();
    }
private static String unPrettyPrint(String s) {
    return s.replace('\u00D7', '*').replace('\u00F7', '/');
}
private void loadHistoryEntry(String rawText) {
    int eq = rawText.indexOf(" = ");
    if (eq < 0) return;
    String left = rawText.substring(0, eq);
    String right = rawText.substring(eq + 3);

    if (left.equals("f(X)")) {
        // Loads the text back into the input for editing; the curve(s) already on the graph
        // are untouched. Pressing EXE again adds this (possibly edited) version as a new curve
        // rather than replacing the original - consistent with EXE always adding in GRAPH mode.
        funcExpr.setLength(0);
        funcExpr.append(unPrettyPrint(right));
        setMode(true);
    } else if (left.length() == 1 && left.charAt(0) >= 'A' && left.charAt(0) <= 'F') {
        active().append(left.charAt(0));
        refreshDisplay();
    } else {
        setMode(false);
        expr.setLength(0);
        expr.append(unPrettyPrint(left));
        refreshDisplay();
    }
}
    private JPanel buildTopPillRow() {
        JPanel row = new JPanel(new GridLayout(1, 4, 6, 0));
        row.setBackground(Theme.APP_BG);
        row.setBorder(new EmptyBorder(4, 10, 4, 10));
 
        shiftButton = pillButton("SHIFT");
        alphaButton = pillButton("ALPHA");
        JButton mode = pillButton("MODE");
        degRadButton = pillButton("RAD");
 
        shiftButton.addActionListener(e -> toggleShift());
        alphaButton.addActionListener(e -> toggleAlpha());
        mode.addActionListener(e -> setMode(!isGraphMode));
        degRadButton.addActionListener(e -> toggleDegRad());
 
        row.add(shiftButton);
        row.add(alphaButton);
        row.add(mode);
        row.add(degRadButton);
        return row;
    }
 
    private JButton pillButton(String text) {
        RoundedButton b = new RoundedButton(text, 999, Theme.PILL_BG, Theme.GOLD, Theme.PILL_BORDER);
        b.setFont(new Font("SansSerif", Font.BOLD, 11));
        b.setPreferredSize(new Dimension(80, 30));
        return b;
    }
 
    //  Scientific function rows (variable column count per row) 
    private JPanel buildSciRow(String[] labels) {
        JPanel row = new JPanel(new GridLayout(1, labels.length, 4, 0));
        row.setBackground(Theme.APP_BG);
        row.setBorder(new EmptyBorder(3, 10, 3, 10));
        for (String label : labels) {
            RoundedButton b;
            if (label.equals("DEL")) {
                b = new RoundedButton(label, 10, Theme.KEY_BG, new Color(230, 120, 110), Theme.DEL_BORDER);
                // Smaller, non-italic font so "DEL" fits comfortably even in an 8-column row,
                // instead of needing a whole extra row (which pushed the window height too tall).
                b.setFont(new Font("SansSerif", Font.BOLD, 10));
            } else {
                b = new RoundedButton(label, 10, Theme.KEY_BG, Theme.GOLD, null);
                b.setFont(new Font("Serif", Font.ITALIC | Font.BOLD, 12));
            }
            b.setPreferredSize(new Dimension(55, 34));
            b.addActionListener(this::onButton);
            if (label.equals("X")) { xButton = b; b.setEnabled(false); }
            if (label.equals("Ans")) ansButton = b;
            if (label.equals("M+")) mPlusButton = b;
            if (label.equals("MR")) mRecallButton = b;
            row.add(b);
        }
        return row;
    }
 
    //  Digit grid: 7 8 9 /, 4 5 6 x, 1 2 3 -, 0 . AC + 
    private JPanel buildDigitGrid() {
        String[][] rows = {
            {"7", "8", "9", "\u00F7"},
            {"4", "5", "6", "\u00D7"},
            {"1", "2", "3", "-"},
            {"0", ".", "AC", "+"}
        };
        JPanel grid = new JPanel(new GridLayout(4, 4, 4, 4));
        grid.setBackground(Theme.APP_BG);
        grid.setBorder(new EmptyBorder(6, 10, 3, 10));
        for (String[] r : rows) {
            for (String label : r) {
                RoundedButton b;
                if (label.equals("AC")) {
                    b = new RoundedButton(label, 10, Theme.AC_RED, Color.WHITE, null);
                } else if (label.equals("\u00F7") || label.equals("\u00D7") || label.equals("-") || label.equals("+")) {
                    b = new RoundedButton(label, 10, Theme.OP_FILL, Theme.GOLD, Theme.GOLD);
                } else {
                    b = new RoundedButton(label, 10, Theme.KEY_BG, Theme.DIGIT_FG, null);
                }
                b.setFont(new Font("SansSerif", Font.BOLD, 15));
                b.setPreferredSize(new Dimension(60, 42));
                b.addActionListener(this::onButton);
                if (label.length() == 1 && label.charAt(0) >= '1' && label.charAt(0) <= '6') {
                    char varLetter = (char) ('A' + (label.charAt(0) - '1'));
                    b.setCornerLabels("\u2192" + varLetter, Theme.SHIFT_LABEL, String.valueOf(varLetter), Theme.ALPHA_LABEL);
                }
                grid.add(b);
            }
        }
        return grid;
    }
     private JPanel buildExeBar() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.APP_BG);
        wrap.setBorder(new EmptyBorder(6, 10, 0, 10));
        RoundedButton exe = new RoundedButton("EXE / EQUAL", 14, Theme.GRAD_LEFT, Color.BLACK, null);
        exe.setGradientTo(Theme.GRAD_RIGHT);
        exe.setFont(new Font("SansSerif", Font.BOLD, 14));
        exe.setPreferredSize(new Dimension(400, 44));
        exe.addActionListener(e -> onExe());
        wrap.add(exe, BorderLayout.CENTER);
        return wrap;
    }
 
    //  Mode / state management 
    private void setMode(boolean graph) {
        isGraphMode = graph;
        cardLayout.show(displayContainer, graph ? "GRAPH" : "COMP");
        compTab.setForeground(graph ? Theme.TEXT_DIM : Theme.GOLD_BRIGHT);
        graphTab.setForeground(graph ? Theme.GOLD_BRIGHT : Theme.TEXT_DIM);
        calcModeValueLabel.setText(graph ? "GRAPH" : "COMP");
        if (xButton != null) xButton.setEnabled(graph);
        if (ansButton != null) ansButton.setEnabled(!graph);
        if (mPlusButton != null) mPlusButton.setEnabled(!graph);
        if (mRecallButton != null) mRecallButton.setEnabled(!graph);
        refreshDisplay();
    }
 
    private void toggleDegRad() {
        useDegrees = !useDegrees;
        String text = useDegrees ? "DEG" : "RAD";
        degRadButton.setText(text);
        angleValueLabel.setText(text);
        graphCanvas.useDegrees = useDegrees; // keep GraphCanvas's own copy in sync
        if (!graphCanvas.functions.isEmpty()) graphCanvas.repaint();
    }
 
    private void toggleShift() {
        shiftMode = !shiftMode;
        ((RoundedButton) shiftButton).setActive(shiftMode);
    }

    private void toggleAlpha() {
        alphaMode = !alphaMode;
        ((RoundedButton) alphaButton).setActive(alphaMode);
    }

    /** Copies whatever is currently shown on the COMP display to the system clipboard,
     *  with a brief gold flash on the display as feedback that the copy happened. */
    private void copyDisplayToClipboard() {
        String text = compDisplay.getText();
        if (text == null || text.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        flashCopyFeedback();
    }

    private void flashCopyFeedback() {
        Color original = compDisplay.getBackground();
        compDisplay.setBackground(Theme.GOLD.darker());
        Timer flash = new Timer(150, e -> compDisplay.setBackground(original));
        flash.setRepeats(false);
        flash.start();
    }
 
    private StringBuilder active() { return isGraphMode ? funcExpr : expr; }
 
    private void refreshDisplay() {
        if (isGraphMode) {
            graphCanvas.inputText = CalcUtils.prettyPrint(funcExpr.toString());
            graphCanvas.repaint();
        } else {
            compDisplay.setText(CalcUtils.prettyPrint(expr.toString()));
        }
    }
 
    private void onButton(ActionEvent e) {
        String cmd = e.getActionCommand();
        StringBuilder buf = active();
        boolean isAlphaDigit = cmd.length() == 1 && cmd.charAt(0) >= '1' && cmd.charAt(0) <= '6';
        if (alphaMode && !isAlphaDigit) {
            alphaMode = false;
            ((RoundedButton) alphaButton).setActive(false);
        }

        switch (cmd) {
            case "1", "2", "3", "4", "5", "6" -> {
                if (alphaMode) {
                    char letter = (char) ('A' + (cmd.charAt(0) - '1')); // 1->A ... 6->F
                    if (shiftMode && !isGraphMode) {
                        storeVariable(letter, buf);
                    } else {
                        buf.append(letter); // RECALL: insert the variable name
                    }
                    alphaMode = false;
                    ((RoundedButton) alphaButton).setActive(false);
                    refreshDisplay();
                } else {
                    buf.append(cmd);
                    refreshDisplay();
                }
            }
            case "AC" -> {
                // In GRAPH mode: first AC clears whatever's currently being typed (normal
                // behavior); a second AC, pressed with nothing left to clear, wipes every
                // plotted curve - a double-tap-to-clear-all pattern.
                boolean bufWasEmpty = buf.length() == 0;
                buf.setLength(0);
                if (isGraphMode && bufWasEmpty) {
                    graphCanvas.functions.clear();
                }
                refreshDisplay();
            }
            case "DEL" -> { if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1); refreshDisplay(); }
            case "sin", "cos", "tan" -> {
                buf.append(shiftMode ? cmd + "h(" : cmd + "(");
                if (shiftMode) { shiftMode = false; ((RoundedButton) shiftButton).setActive(false); }
                refreshDisplay();
            }
            case "ln", "log", "sqrt", "cbrt" -> { buf.append(cmd).append("("); refreshDisplay(); }
            case "x^2" -> { buf.append("^2"); refreshDisplay(); }
            case "x^-1" -> { buf.insert(0, "1/(").append(")"); refreshDisplay(); }
            case "10^x" -> { buf.append("10^"); refreshDisplay(); }
            case "^", "(", ")", "pi", "e" -> { buf.append(cmd); refreshDisplay(); }
            case "X" -> { if (isGraphMode) buf.append("X"); refreshDisplay(); }
            case "Ans" -> { buf.append(CalcUtils.trimZero(lastAnswer)); refreshDisplay(); }
            case "M+" -> addToMemory();
            case "MR" -> { buf.append(CalcUtils.trimZero(memory)); refreshDisplay(); }
            case "\u00F7" -> { buf.append("/"); refreshDisplay(); }
            case "\u00D7" -> { buf.append("*"); refreshDisplay(); }
            case "\u00B1" -> toggleSign();
            case "%" -> applyPercent();
            default -> { buf.append(cmd); refreshDisplay(); } // digits, '.', '+', '-'
        }
    }
    private void toggleSign() {
    StringBuilder buf = active();
    String s = buf.toString();
    if (s.isEmpty()) return;

    if (s.equals("-")) { buf.setLength(0); refreshDisplay(); return; }

    // Already wrapped as "(-NUMBER)" at the end? Unwrap it back to positive.
    if (s.endsWith(")")) {
        int closeIdx = s.length() - 1;
        int j = closeIdx - 1;
        while (j >= 0 && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j--;
        if (j >= 1 && s.charAt(j) == '-' && s.charAt(j - 1) == '(') {
            buf.replace(j - 1, s.length(), s.substring(j + 1, closeIdx));
            refreshDisplay();
            return;
        }
    }

    int start = s.length();
    while (start > 0 && (Character.isDigit(s.charAt(start - 1)) || s.charAt(start - 1) == '.')) start--;
    if (start == s.length()) { buf.append('-'); refreshDisplay(); return; } // nothing typed yet - start a negative

    if (start == 1 && s.charAt(0) == '-') {
        buf.deleteCharAt(0); // bare leading "-3" -> "3"
    } else if (start == 0) {
        buf.insert(0, '-'); // bare "3" -> "-3"
    } else {
        buf.insert(start, "(-").append(")"); // "5*3" -> "5*(-3)"
    }
    refreshDisplay();
}
private void applyPercent() {
    StringBuilder buf = active();
    String s = buf.toString();
    int start = s.length();
    while (start > 0 && (Character.isDigit(s.charAt(start - 1)) || s.charAt(start - 1) == '.')) start--;
    if (start == s.length()) return; // buffer doesn't end in a number - nothing to convert
    buf.replace(start, s.length(), "(" + s.substring(start) + "/100)");
    refreshDisplay();
}

    /** Evaluates the current buffer (or falls back to lastAnswer if empty) and stores the result
     *  into the given A-F variable slot. Called from ALPHA+SHIFT+digit. COMP mode only - a graph
     *  function has no single value to store. */
    private void storeVariable(char letter, StringBuilder buf) {
        double value;
        try {
            value = buf.length() > 0 ? new Evaluator(buf.toString(), useDegrees, 0, variables).evaluate() : lastAnswer;
        } catch (Exception ex) {
            compDisplay.setText("Error");
            shiftMode = false;
            ((RoundedButton) shiftButton).setActive(false);
            return;
        }
        variables.put(letter, value);
        String text = letter + " = " + CalcUtils.trimZero(value);
        compDisplay.setText(text);
        history.add(text);
        updateHistoryPanel();
        buf.setLength(0);
        shiftMode = false;
        ((RoundedButton) shiftButton).setActive(false);
    }
 
    private void onExe() {
        if (isGraphMode) plotFunction(); else calculate();
    }
 
    private void calculate() {
        if (expr.length() == 0) return;
        try {
            String exprText = expr.toString();
            double result = new Evaluator(exprText, useDegrees, 0, variables).evaluate();
            String text = Double.isNaN(result) || Double.isInfinite(result) ? "Error" : CalcUtils.trimZero(result);
            if (!text.equals("Error")) {
                lastAnswer = result;
                history.add(CalcUtils.prettyPrint(exprText) + " = " + text);
                updateHistoryPanel();
            }
            compDisplay.setText(text);
            expr.setLength(0);
            expr.append(text);
        } catch (Exception ex) {
            compDisplay.setText("Error" + (ex.getMessage() != null ? ": " + ex.getMessage() : ""));
            expr.setLength(0);
        }
    }
 
    private void plotFunction() {
        if (funcExpr.length() == 0) return;
        // Adds a new curve rather than replacing the old one, so f1(X), f2(X), etc. all stay
        // visible together. Color cycles through Theme.GRAPH_COLORS by plot order.
        Color color = Theme.GRAPH_COLORS[graphCanvas.functions.size() % Theme.GRAPH_COLORS.length];
        graphCanvas.functions.add(new GraphCanvas.PlottedFunction(funcExpr.toString(), color));
        history.add("f(X) = " + CalcUtils.prettyPrint(funcExpr.toString()));
        updateHistoryPanel();
        // Clear the input so the next thing typed starts a fresh function instead of editing
        // the one that was just plotted - the plotted curve itself is unaffected by this.
        funcExpr.setLength(0);
        graphCanvas.repaint();
        refreshDisplay();
    }
 
    private void addToMemory() {
        if (expr.length() == 0) return;
        try {
            double val = new Evaluator(expr.toString(), useDegrees, 0, variables).evaluate();
            memory += val;
            lastAnswer = val;
            String text = CalcUtils.trimZero(val);
            compDisplay.setText(text);
            expr.setLength(0);
            expr.append(text);
        } catch (Exception ex) {
            compDisplay.setText("Error");
            expr.setLength(0);
        }
    }
      // rmdir /s /q Calculator spring 
    // javac -d . CalculatorApp\*.java
    // "C:\Program Files\Java\jdk-19\bin\jar" cvfm input_dir\Calculator.jar manifest.txt CalculatorApp
    // "C:\Program Files\Java\jdk-19\bin\jpackage" --input input_dir --name "Calculator" --main-jar Calculator.jar --main-class CalculatorApp.Calculator --type app-image
    // toDo : fix the funcitonality mistakes and add the hover
 
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "gasp");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new Calculator().setVisible(true));
    }
}