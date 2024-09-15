package org.atcraftmc.mvdl;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public final class SyncDatapackCommand implements CommandExecutor {
    private final MVDataPackLoader plugin;

    public SyncDatapackCommand(MVDataPackLoader plugin) {
        this.plugin = plugin;
    }

    static String mc(String str) {
        return str.toLowerCase().replace("-", "_");
    }

    public void copyFolder(File source, File dest, int deep, String packName, String worldName) throws IOException {
        var space = " ".repeat(deep * 2);
        var type = source.isFile() ? "[F]" : "[d]";

        if (source.isFile()) {
            dest.getParentFile().mkdirs();
            dest.createNewFile();

            try (var in = new FileInputStream(source); var out = new FileOutputStream(dest)) {
                var buffer = in.readAllBytes();
                var content = new String(buffer, StandardCharsets.UTF_8);

                if (source.getName().endsWith(".json") && !content.contains("predicate")) {
                    content = content.replace(packName, "%s_%s".formatted(mc(worldName), packName));
                }
                if (source.getName().endsWith(".mcfunction")) {
                    content = content.replaceAll("execute ", "execute in %s ".formatted(worldName));
                    content = content.replaceAll("function ", "function %s_".formatted(mc(worldName)));
                    content = content.replace("tag ","execute in %s run tag ".formatted(worldName));
                    content = content.replace("\ntp ","execute in %s run tp ".formatted(worldName));
                }


                buffer = content.getBytes(StandardCharsets.UTF_8);
                out.write(buffer);
            }

            return;
        }

        dest.mkdirs();

        for (var f : Objects.requireNonNull(source.listFiles())) {
            var name = f.getName();

            if (name.equals(packName)) {
                name = mc(worldName) + "_" + packName;
            }

            var dir = new File(dest.getAbsolutePath() + "/" + name);

            copyFolder(f, dir, deep + 1, packName, worldName);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        for (var world : Bukkit.getWorlds()) {
            if (!this.plugin.isMVWorld(world)) {
                continue;
            }

            copyPackOfWorld(world);

            commandSender.sendMessage("loaded datapack(s) of world " + world);
        }

        return true;
    }

    private void copyPackOfWorld(World world) {
        var worldId = world.getName();
        var server = System.getProperty("user.dir");
        var datapackFolder = new File("%s/%s/datapacks".formatted(server, world.getName()));

        if (!datapackFolder.exists()) {
            return;
        }


        try {
            for (var folder : Objects.requireNonNull(datapackFolder.listFiles())) {
                var id = folder.getName();
                var dest = new File("%s/world/datapacks/%s_%s".formatted(server, mc(worldId), id)); // 复制的目标文件夹路径

                System.out.println(folder.getAbsolutePath());

                // 如果目标目录不存在，则创建它
                if (!Files.exists(dest.toPath())) {
                    Files.createDirectories(dest.toPath());
                }

                // 使用 Files.walkFileTree 递归复制文件和目录
                copyFolder(folder, dest, 0, id, worldId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final class FileVisitor extends SimpleFileVisitor<Path> {
        private final String worldId;
        private final String datapackId;
        private final Path dest;
        private final Path target;

        private FileVisitor(String worldId, String datapackId, Path dest, Path target) {
            this.worldId = worldId;
            this.datapackId = datapackId;
            this.dest = dest;
            this.target = target;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            // 计算出要复制到的目标路径
            Path targetFile = this.dest.resolve(this.target.relativize(file));
            // 复制文件
            Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // 计算出要复制到的目录路径

            var path = this.target.relativize(dir);
            var val = path.toFile().getName();

            if (val.equals(this.datapackId)) {
                path = path.resolve(val.replace(this.datapackId, this.worldId.replace("-", "_") + "_" + this.datapackId));
            }

            Path targetDir = dest.resolve(path);


            // 如果目录不存在，创建目录
            if (!Files.exists(targetDir)) {
                Files.createDirectory(targetDir);
            }
            return FileVisitResult.CONTINUE;
        }
    }
}
