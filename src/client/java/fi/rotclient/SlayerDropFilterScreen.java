package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Per-drop Slayer filter editor backed by stable SkyBlock item ids. */
final class SlayerDropFilterScreen extends Screen {
    private final Screen parent;
    private SlayerPolicy.SlayerType type = SlayerPolicy.SlayerType.REVENANT;
    private int page;

    SlayerDropFilterScreen(Screen parent) {
        super(Component.literal("Slayer Drop Filter"));
        this.parent = parent;
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        int x = Math.max(10, (width - 720) / 2), y = Math.max(10, (height - 440) / 2);
        RotClientUiDraw.drawShadowedPanel(g, x, y, 720, 440);
        RotClientUiDraw.drawHeaderBar(g, font, x, y, 720, 38,
                "Slayer Drop Filter", "Select the exact drops used by alerts and since-last tracking");
        int tabY = y + 50;
        SlayerPolicy.SlayerType[] types = SlayerPolicy.SlayerType.values();
        for (int i = 0; i < types.length; i++) {
            int tx = x + 14 + i * 114;
            if (types[i] == type) g.fill(tx, tabY - 3, tx + 106, tabY + 18, RotClientTheme.SELECTED_ROW);
            RotClientUiDraw.text(g, font, types[i].displayName(), tx + 5, tabY,
                    types[i] == type ? RotClientTheme.HUD_ACCENT : RotClientTheme.TEXT_MUTED, true);
        }
        List<String> ids = ids(); int start = page * 18;
        for (int i = 0; i < 18 && start + i < ids.size(); i++) {
            String id = ids.get(start + i); int col = i % 2, row = i / 2;
            int rx = x + 24 + col * 338, ry = y + 88 + row * 31;
            boolean selected = selected(id);
            g.fill(rx, ry, rx + 322, ry + 24, selected ? 0xFF18384A : RotClientTheme.SURFACE_ALT);
            g.fill(rx + 5, ry + 6, rx + 17, ry + 18, selected ? RotClientTheme.SUCCESS : 0xFF334455);
            RotClientUiDraw.text(g, font, title(id), rx + 24, ry + 7, selected ? RotClientTheme.TEXT : RotClientTheme.TEXT_MUTED, false);
        }
        button(g, mx, my, x + 24, y + 398, 105, "Select all");
        button(g, mx, my, x + 135, y + 398, 105, "Clear all");
        button(g, mx, my, x + 470, y + 398, 70, "< Prev");
        button(g, mx, my, x + 546, y + 398, 70, "Next >");
        button(g, mx, my, x + 622, y + 398, 82, "Done");
        super.extractRenderState(g, mx, my, delta);
    }

    @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
        if (e.button() != 0) return super.mouseClicked(e, doubleClick);
        int mx=(int)Math.round(e.x()), my=(int)Math.round(e.y());
        int x=Math.max(10,(width-720)/2), y=Math.max(10,(height-440)/2);
        SlayerPolicy.SlayerType[] types=SlayerPolicy.SlayerType.values();
        for(int i=0;i<types.length;i++) if(inside(mx,my,x+14+i*114,y+47,106,21)){type=types[i];page=0;return true;}
        List<String> ids=ids(); int start=page*18;
        for(int i=0;i<18&&start+i<ids.size();i++){int rx=x+24+(i%2)*338,ry=y+88+(i/2)*31;
            if(inside(mx,my,rx,ry,322,24)){toggle(ids.get(start+i));return true;}}
        if(inside(mx,my,x+24,y+398,105,24)){selectFamily(true);return true;}
        if(inside(mx,my,x+135,y+398,105,24)){selectFamily(false);return true;}
        if(inside(mx,my,x+470,y+398,70,24)){page=Math.max(0,page-1);return true;}
        if(inside(mx,my,x+546,y+398,70,24)){page=Math.min(Math.max(0,(ids.size()-1)/18),page+1);return true;}
        if(inside(mx,my,x+622,y+398,82,24)){onClose();return true;}
        return super.mouseClicked(e,doubleClick);
    }

    @Override public void onClose(){RotClientClient.save();Minecraft.getInstance().gui.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    private List<String> ids(){return SlayerDropScalePolicy.dropsFor(type).stream().sorted().toList();}
    private boolean selected(String id){return SlayerDropScalePolicy.selected(extras().slayerDropFilter,id);}
    private void materializeDefaults(){if(extras().slayerDropFilter.isEmpty())extras().slayerDropFilter=new ArrayList<>(SlayerDropScalePolicy.allDrops());}
    private void toggle(String id){materializeDefaults();if(!extras().slayerDropFilter.remove(id))extras().slayerDropFilter.add(id);RotClientClient.save();}
    private void selectFamily(boolean value){materializeDefaults();for(String id:ids()){extras().slayerDropFilter.remove(id);if(value)extras().slayerDropFilter.add(id);}RotClientClient.save();}
    private void button(GuiGraphicsExtractor g,int mx,int my,int x,int y,int w,String label){RotClientUiDraw.drawButton(g,font,mx,my,x,y,w,label,false,true);}
    private static boolean inside(int mx,int my,int x,int y,int w,int h){return mx>=x&&mx<x+w&&my>=y&&my<y+h;}
    private static String title(String id){String s=id.toLowerCase(java.util.Locale.ROOT).replace('_',' ');return Character.toUpperCase(s.charAt(0))+s.substring(1);}
    private static QolSkyblockExtras extras(){return RotClientClient.qolConfigPublic().extras();}
}
