package Parser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.ArrayList;

public class Editor extends JFrame {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private LineNumberTextPane inputTextPane = new LineNumberTextPane();
    private JTextPane errorTextPane = new JTextPane();
    private StyledDocument document = (StyledDocument) inputTextPane.getDocument();
    private SimpleAttributeSet attributes = new SimpleAttributeSet();
    private JFileChooser fileChooser = new JFileChooser(); // 文件选择器
    private Lexer lexer = new Lexer();
    private LALRParser lalrParser = new LALRParser();

    private Editor() {
        super("Mini-C Editor");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        contentPane.setLayout(new BorderLayout(0, 0));
        setContentPane(contentPane);

        JScrollPane jScrollPane = new JScrollPane();
        contentPane.add(jScrollPane, BorderLayout.CENTER);
        jScrollPane.setViewportView(inputTextPane);
        inputTextPane.setFont(new Font("仿宋", Font.PLAIN, 24));
        errorTextPane.setFont(new Font("仿宋", Font.PLAIN, 24));

        contentPane.add(errorTextPane, BorderLayout.SOUTH);

        initJMenuBar();

        setSize(1300, 1500);
        setVisible(true);
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                Editor frame = new Editor();
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void initJMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu menuFile = new JMenu("文件(F)");
        menuFile.setMnemonic('F');
        JMenu menuEdit = new JMenu("编辑(E)");
        menuEdit.setMnemonic('E');
        JMenu menuRun = new JMenu("运行(R)");
        menuRun.setMnemonic('R');
        JMenu menuAbout = new JMenu("帮助(H)");
        menuAbout.setMnemonic('H');

        JMenuItem menuItemCreate = new JMenuItem("新建(N)");
        menuItemCreate.setMnemonic('N');
        menuItemCreate.setAccelerator(KeyStroke.getKeyStroke("control N"));
        menuItemCreate.addActionListener(e -> inputTextPane.setText(""));

        JMenuItem menuItemOpen = new JMenuItem("打开(O)");
        menuItemOpen.setMnemonic('O');
        menuItemOpen.setAccelerator(KeyStroke.getKeyStroke("control O"));
        menuItemOpen.addActionListener(e -> {
            if (fileChooser.showOpenDialog(Editor.this) == JFileChooser.APPROVE_OPTION) // 点击对话框打开选项
            {
                File f = fileChooser.getSelectedFile(); // 得到选择的文件
                try {
                    StringBuilder sb = new StringBuilder();
                    String s;
                    BufferedReader br = new BufferedReader(new FileReader(f));

                    while ((s = br.readLine()) != null)
                        sb.append(s).append("\r\n");
                    s = sb.toString();
                    br.close();

                    inputTextPane.setText(s);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JMenuItem menuItemSave = new JMenuItem("保存(S)");
        menuItemSave.setMnemonic('S');
        menuItemSave.setAccelerator(KeyStroke.getKeyStroke("control S"));
        menuItemSave.addActionListener(e -> {
            if (fileChooser.showSaveDialog(Editor.this) == JFileChooser.APPROVE_OPTION) {
                File f = fileChooser.getSelectedFile();
                try {
                    FileOutputStream out = new FileOutputStream(f);
                    out.write(inputTextPane.getText().getBytes());
                    out.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JMenuItem menuItemExit = new JMenuItem("退出(X)");
        menuItemExit.setMnemonic('X');
        menuItemExit.addActionListener(e -> dispose());

        JMenuItem menuItemCut = new JMenuItem("剪切(T)");
        menuItemCut.setMnemonic('T');
        menuItemCut.setAccelerator(KeyStroke.getKeyStroke("control X"));
        menuItemCut.addActionListener(e -> inputTextPane.cut());

        JMenuItem menuItemCopy = new JMenuItem("复制(C)");
        menuItemCopy.setMnemonic('C');
        menuItemCopy.setAccelerator(KeyStroke.getKeyStroke("control C"));
        menuItemCopy.addActionListener(e -> inputTextPane.copy());

        JMenuItem menuItemPaste = new JMenuItem("粘贴(P)");
        menuItemPaste.setMnemonic('P');
        menuItemPaste.setAccelerator(KeyStroke.getKeyStroke("control V"));
        menuItemPaste.addActionListener(e -> inputTextPane.paste());

        JMenuItem menuItemRun = new JMenuItem("运行(R)");
        menuItemRun.setMnemonic('R');
        menuItemRun.setAccelerator(KeyStroke.getKeyStroke("control R"));
        menuItemRun.addActionListener(this::actionRunPerformed);

        JMenuItem menuItemAbout = new JMenuItem("关于Mini-C(A)");
        menuItemAbout.setMnemonic('A');
        menuItemAbout.addActionListener(e -> JOptionPane.showMessageDialog(Editor.this, "第15组哦\r\n实现了Mini-C的词法语法分析", "关于",
                JOptionPane.PLAIN_MESSAGE));

        menuFile.add(menuItemCreate);
        menuFile.add(menuItemOpen);
        menuFile.add(menuItemSave);
        menuFile.add(menuItemExit);
        menuEdit.add(menuItemCut);
        menuEdit.add(menuItemCopy);
        menuEdit.add(menuItemPaste);
        menuRun.add(menuItemRun);
        menuAbout.add(menuItemAbout);
        menuBar.add(menuFile);
        menuBar.add(menuEdit);
        menuBar.add(menuRun);
        menuBar.add(menuAbout);
        setJMenuBar(menuBar);
    }

    @SuppressWarnings("unused")
    private void actionRunPerformed(ActionEvent e) {
        boolean ValidSourceFile = true;// 当源文件有错时设为false
        String text = null;
        try {
            text = inputTextPane.getText();
            if (text == null)
                return;
            System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("out.txt")))));
            System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(new File("err.txt")))));
        } catch (FileNotFoundException e1) {
            System.err.println(e1.getMessage());
            System.exit(-1);
        }

        lalrParser.StartParsing(lexer.StartLexicalAnalysis(text));
        Node root = lalrParser.getRoot();
        if (root != null)
            Node.print(root, null);
        System.out.flush();
        System.err.flush();

        ArrayList<Token> tokens = lalrParser.getTokens();
        for (Token token : tokens) {
            Color color;
            if (!token.isValid()) {
                ValidSourceFile = false;
                color = Color.RED;
            } else {
                if (token.getType() == TokenType.KEYWORD)
                    color = Color.BLUE;
                else if (token.getType() == TokenType.CHARACTER || token.getType() == TokenType.STRINGLITERAL)
                    color = Color.GREEN;
                else if (token.getType() == TokenType.COMMENT)
                    color = Color.LIGHT_GRAY;
                else if (token.getType() == TokenType.INTEGER || token.getType() == TokenType.FLOAT
                        || token.getType() == TokenType.DOUBLE)
                    color = Color.ORANGE;
                else
                    color = Color.BLACK;
            }
            StyleConstants.setForeground(attributes, color);
            document.setCharacterAttributes(token.getPosition() - token.getLine() + 1, token.getValue().length(), attributes, false);
        }

        try {
            StringBuilder sb = new StringBuilder();
            String s;
            BufferedReader br = new BufferedReader(new FileReader(new File("err.txt")));

            while ((s = br.readLine()) != null)
                sb.append(s).append("\r\n");
            s = sb.toString();
            br.close();

            errorTextPane.setText(sb.toString());
            StyledDocument document = (StyledDocument) errorTextPane.getDocument();
            StyleConstants.setForeground(attributes, Color.RED);
            document.setCharacterAttributes(0, s.length(), attributes, false);
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        try {
            if (ValidSourceFile)
                Runtime.getRuntime().exec("C:\\WINDOWS\\system32\\notepad.exe out.txt");
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}

class LineNumberTextPane extends JTextPane {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    LineNumberTextPane() {
        super();
    }

    public void paint(Graphics g) {
        super.paint(g);
        setMargin(new Insets(0, 55, 0, 0));
        g.setColor(new Color(180, 180, 180));// 背景颜色
        g.fillRect(0, 0, 45, getHeight());

        int rows = getStyledDocument().getDefaultRootElement().getElementCount();
        g.setColor(new Color(90, 90, 90));// 行号颜色
        for (int row = 1; row <= rows; row++) {
            g.drawString(String.valueOf(row), 3, row * 29);
        }
    }
}