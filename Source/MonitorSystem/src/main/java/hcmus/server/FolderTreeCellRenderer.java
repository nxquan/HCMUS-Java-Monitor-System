package hcmus.server;

import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class FolderTreeCellRenderer extends DefaultTreeCellRenderer {

    private final ImageIcon folderIcon;
    private final ImageIcon openFolderIcon;

    public FolderTreeCellRenderer() {
        folderIcon = new ImageIcon(getClass().getResource("folder.png"));
        openFolderIcon = new ImageIcon(getClass().getResource("open.png")); 
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        setIcon(expanded ? openFolderIcon : folderIcon);
        return this;
    }
}
