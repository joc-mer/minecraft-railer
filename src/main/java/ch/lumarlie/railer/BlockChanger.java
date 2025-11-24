package ch.lumarlie.railer;

import lombok.NonNull;
import org.bukkit.Location;
import org.bukkit.Material;

public interface BlockChanger {
    record BlockChange(Location location, Material before, Material after) {}

    BlockChange changeBlock(@NonNull Location location, @NonNull Material newMaterial);
    void undo(BlockChange blockChange);

    class DefaultBlockChanger implements BlockChanger {
        @Override
        public BlockChange changeBlock(Location location, Material newMaterial) {
            var currentMaterial = location.getBlock().getType();
            location.getBlock().setType(newMaterial);
            return new BlockChange(location, currentMaterial, newMaterial);
        }

        @Override
        public void undo(BlockChange blockChange) {
            blockChange.location.getBlock().setType(blockChange.before);
        }
    }

}
