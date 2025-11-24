package ch.lumarlie.railer;

import org.bukkit.Material;

public interface MaterialChecker {
    boolean isSolid(Material material);

    boolean isAir(Material material);

    boolean isRail(Material material);

    boolean isWater(Material material);

    int getMaxDurability(Material material);

    boolean isCompostable(Material material);

    boolean isEmpty(Material material);

    class Default implements MaterialChecker {
        @Override
        public boolean isSolid(Material material) {
            return material.isSolid();
        }

        @Override
        public boolean isAir(Material material) {
            return material.isAir();
        }

        @Override
        public boolean isRail(Material material) {
            return material == Material.RAIL || material == Material.POWERED_RAIL;
        }

        @Override
        public boolean isWater(Material material) {
            return material == Material.WATER;
        }

        @Override
        public int getMaxDurability(Material material) {
            return material.getMaxDurability();
        }

        @Override
        public boolean isCompostable(Material material) {
            return material.isCompostable();
        }

        @Override
        public boolean isEmpty(Material material) {
            return material.isEmpty();
        }
    }
}
