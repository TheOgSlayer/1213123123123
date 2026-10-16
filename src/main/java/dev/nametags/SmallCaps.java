package dev.nametags;

public final class SmallCaps {
    private static final String NORMAL = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL  = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡхʏᴢ";

    private SmallCaps() {}

    public static String convert(String in) {
        StringBuilder sb = new StringBuilder();
        for (char c : in.toCharArray()) {
            int i = NORMAL.indexOf(Character.toLowerCase(c));
            sb.append(i >= 0 ? SMALL.charAt(i) : c);
        }
        return sb.toString();
    }
}
