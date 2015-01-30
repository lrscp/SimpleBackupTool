package images;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ImageResource {

    private static Image mDirImage;

    private static Image mFileImage;

    private static Image mCheckImage;

    public static Image getDirIcon() {
        if (mDirImage == null) {
            mDirImage = new Image(Display.getDefault(), ImageResource.class.getResourceAsStream("icon_ClosedFolder.gif"));
        }
        return mDirImage;
    }

    public static Image getFileIcon() {
        if (mFileImage == null) {
            mFileImage = new Image(Display.getDefault(), ImageResource.class.getResourceAsStream("icon_File.gif"));
        }
        return mFileImage;
    }

    public static Image getCheckIcon() {
        if (mCheckImage == null) {
            mCheckImage = new Image(Display.getDefault(), ImageResource.class.getResourceAsStream("icon_has_ignore.png"));
        }
        return mCheckImage;
    }
}
