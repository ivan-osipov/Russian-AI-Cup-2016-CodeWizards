import java.awt.*;

public class DebugVisualizer {

    private Object visualClient;
    private Class<?> visualClientClass;
    private boolean initialized = false;

    public DebugVisualizer() {
        try {
            visualClientClass = Class.forName("debug.VisualClient");
            if(visualClientClass != null) {
                visualClient = visualClientClass.newInstance();
                initialized = true;
            }
        } catch (Exception e) {
            visualClient = null;
            e.printStackTrace();
        }
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void drawRectangle(double x1, double y1, double x2, double y2, Color color) {
        post(() -> {
            try {
                visualClientClass.getMethod("rect", double.class, double.class, double.class, double.class, Color.class)
                        .invoke(visualClient, x1, y1, x2, y2, color);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void fillRectangle(double x1, double y1, double x2, double y2, Color color) {
        pre(() -> {
            try {
                visualClientClass.getMethod("fillRect", double.class, double.class, double.class, double.class, Color.class)
                        .invoke(visualClient, x1, y1, x2, y2, color);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void fillCircle(double x, double y, double r, Color color) {
        pre(() -> {
            try {
                visualClientClass.getMethod("fillCircle", double.class, double.class, double.class, Color.class)
                        .invoke(visualClient, x, y, r, color);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void text(double x, double y, String text, Color color) {
//        post(() -> {
            try {
                visualClientClass.getMethod("text", double.class, double.class, String.class, Color.class)
                        .invoke(visualClient, x, y, text, color);
            } catch (Exception e) {
                e.printStackTrace();
            }
//        });
    }

    public void drawZone(Zone zone, Color color) {
        post(() -> {
            try {
                for (Line2D line : zone.getLines()) {
                    visualClientClass.getMethod("line", double.class, double.class, double.class, double.class, Color.class)
                            .invoke(visualClient, line.getStart().getX(), line.getStart().getY(), line.getEnd().getX(), line.getEnd().getY(), color);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    public void drawZones(Color color, Zone... zones) {
//        post(() -> {
            try {
                for (Zone zone : zones) {
                    for (Line2D line : zone.getLines()) {
                        visualClientClass.getMethod("line", double.class, double.class, double.class, double.class, Color.class)
                                .invoke(visualClient, line.getStart().getX(), line.getStart().getY(), line.getEnd().getX(), line.getEnd().getY(), color);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
//        });
    }

    public void post(Runnable runnable) {
        if(isInitialized()) {
            beginPost();
            runnable.run();
            endPost();
        }
    }

    public void pre(Runnable runnable) {
        if(isInitialized()) {
            beginPre();
            runnable.run();
            endPre();
        }
    }

    private void beginPre() {
        try {
            visualClientClass.getMethod("beginPre").invoke(visualClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void beginPost() {
        try {
            visualClientClass.getMethod("beginPost").invoke(visualClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endPre() {
        try {
            visualClientClass.getMethod("endPre").invoke(visualClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endPost() {
        try {
            visualClientClass.getMethod("endPost").invoke(visualClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endAbs() {
        try {
            visualClientClass.getMethod("endAbs").invoke(visualClient);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
