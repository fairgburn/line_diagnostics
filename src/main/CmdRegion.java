package main;

import info.Debug;
import info.Globals;

import java.awt.*;
import java.util.LinkedList;

/**
 * Created by acs on 7/26/17.
 */
public class CmdRegion
    extends Region
{

    // info
    private Globals glob = Globals.getInstance();

/** members and constructors **/

    private Surface surface = null;
    private Color backgroundColor = glob.cmdColor;
    private boolean attached = false;
    private LinkedList<Button> buttonsList = new LinkedList<>();
    private int padding = glob.padding;

    /***/

    public CmdRegion(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public CmdRegion(Region r) {
        super(r);
    }

    public CmdRegion() {}



/** methods **/

    // copy button clicked
    // save the selected zone to clipboard
    private void copy() {
        // need attachment to surface to see selected zone
        if (!attached) {
            Debug.log("attempted to copy from unattached CmdRegion");
            return;
        }

        // if a zone is selected, copy its unit
        //
        // if no zone is selected or the selected zone has nothing in it,
        //   a NullPointerException will be thrown - catch this and set
        //   clipboard to null
        try {
            surface.setClipboard(surface.getSelectedZone().getUnit().getInfo());
        } catch (NullPointerException e) {
            surface.setClipboard(null);
        }

    }

    // paste button clicked
    // put clipboard contents in selected zone
    private void paste() {
        // need attachment to surface to see selected zone
        if (!attached) {
            Debug.log("attempted to paste from unattached CmdRegion");
            return;
        }

        if (surface.getSelectedZone() == null) {
            Debug.log("attempted to paste but no zone is selected");
            return;
        }

        // don't do this if clipboard is empty
        if (surface.getClipboard() == null) return;

        // paste the unit from clipboard into selected zone (SQL)
        UnitInfo ui = surface.getClipboard();
        Zone sz = surface.getSelectedZone();
        // empty string if null
        String customer = (ui.customer == null) ? "" : ui.customer;
        String sql = "UPDATE inventory SET order_num=" + ui.order +
                ", customer='" + customer + "'" +
                ", width=" + ui.width +
                ", length=" + ui.length +
                " WHERE id=" + sz.getLineNum() + " AND " +
                " zone=" + sz.getZoneNum();

        Debug.log(sql);
        surface.getDB().executeUpdate(sql);
    }

    // delete button clicked
    // remove contents from zone (SQL)
    private void delete() {
        if (!attached) {
            Debug.log("attempted to delete from unattached CmdRegion");
            return;
        }

        // don't do anything if no zone is selected
        if (surface.getSelectedZone() == null) return;

        Zone z = surface.getSelectedZone();
        // delete the unit
        // only need to set order_num=0 (order_num > 0 is how we check for inventory)
        String sql = "UPDATE inventory SET order_num=0 " + //, customer='', width=0, length=0 " +
                "WHERE id=" + z.getLineNum() +
                " AND zone=" + z.getZoneNum();
        surface.getDB().executeUpdate(sql);
        surface.getSelectedZone().setUnit(null);
    }

/** superclass **/

    // received a signal from an input driver
    @Override
    public void touch(Point p) {
        if (!super.contains(p)) return;

        // send click signal to buttons
        for (Button b : buttonsList) {
            if (b.contains(p)) b.click();
        }
    }

    // attach this region to the Surface above it
    @Override
    public void attach(Surface s) {
        surface = s;
        attached = true;
    }

    // draw region on the screen
    @Override
    public void draw(Graphics2D g) {

        Color oldColor = g.getColor();
        g.setColor(backgroundColor);
        super.fill(g);

        int num_buttons = 3; // so we can change this later

        /** draw buttons as squares in a vertical line **/

        int button_size = (this.height / num_buttons) - (glob.padding >> 1);
        int button_spacing = this.height / (num_buttons + 1);
        int[] button_y_arr = new int[num_buttons];
        for (int i = 1; i <= num_buttons; i++) {
            int by = i * button_spacing + this.y;
            button_y_arr[i - 1] = by - (button_size >> 1);
        }


        /*
        Button: abstract class, override click() method at creation time to
            give it a function. Or you could extend it, but that is probably
            overkill.
        */

        // should do this more elegantly
        // right now a new Button object is created every time the screen is drawn
        buttonsList.clear();

        // draw the buttons in the middle of the region
        int button_x = this.x + (this.width >> 1) - (button_size >> 1);

        Button copyButton = new Button(
                button_x,
                button_y_arr[0] ,
                button_size,
                button_size,
                "COPY") {
            @Override
            public void click() { copy(); }

        };
        copyButton.draw(g);
        buttonsList.addLast(copyButton);

        Button pasteButton = new Button(
                button_x,
                button_y_arr[1] ,
                button_size,
                button_size,
                "PASTE") {
            @Override
            public void click() { paste(); }

        };
        pasteButton.draw(g);
        buttonsList.addLast(pasteButton);

        Button deleteButton = new Button(
                button_x,
                button_y_arr[2] ,
                button_size,
                button_size,
                "DELETE") {
            @Override
            public void click() { delete(); }

        };
        deleteButton.draw(g);
        buttonsList.addLast(deleteButton);


    }

}
