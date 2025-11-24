package ch.lumarlie.railer;

import net.kyori.adventure.identity.Identity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RailerEventListenerTest {

    @Mock
    Logger logger;
    @Mock
    World world;
    @Mock
    Player player;
    @Mock
    Identity identity;
    @Mock
    BlockPlaceEvent event;
    @Mock
    Block block;
    @Mock
    MaterialChecker materialChecker;
    @Spy
    BlockChanger blockChanger = new BlockChanger.DefaultBlockChanger();

    RailerEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RailerEventListener(logger, materialChecker, blockChanger);
        lenient().when(event.getPlayer()).thenReturn(player);
        lenient().when(player.identity()).thenReturn(identity);
    }

    @Test
    void should_not_trigger_pathfinding_when_too_close() {
        // Given - First Rail
        BlockPlaceEvent event1 = mock(BlockPlaceEvent.class);
        Block block1 = mock(Block.class);
        Block blockPlaced1 = mock(Block.class);
        when(event1.getPlayer()).thenReturn(player);
        when(event1.getBlock()).thenReturn(block1);
        when(event1.getBlockPlaced()).thenReturn(blockPlaced1);
        when(block1.getType()).thenReturn(Material.RAIL);

        Location loc1 = new Location(world, 1d, 1d, 1d);
        when(blockPlaced1.getLocation()).thenReturn(loc1);

        listener.onPlayerBlock(event1);

        // Given - Second Rail nearby (distance 2 < 8)
        BlockPlaceEvent event2 = mock(BlockPlaceEvent.class);
        Block block2 = mock(Block.class);
        Block blockPlaced2 = mock(Block.class);
        when(event2.getPlayer()).thenReturn(player);
        when(event2.getBlock()).thenReturn(block2);
        when(event2.getBlockPlaced()).thenReturn(blockPlaced2);
        when(block2.getType()).thenReturn(Material.RAIL);

        Location loc2 = new Location(world, 3d, 1d, 1d);
        when(blockPlaced2.getLocation()).thenReturn(loc2);

        // When
        listener.onPlayerBlock(event2);

        // Then
        verify(blockChanger, never()).changeBlock(any(), any());
    }

    @Test
    void should_trigger_pathfinding_on_second_rail_placement_far_enough() {
        // Given - First Rail
        BlockPlaceEvent event1 = mock(BlockPlaceEvent.class);
        Block block1 = mock(Block.class);
        Block blockPlaced1 = mock(Block.class);
        when(event1.getPlayer()).thenReturn(player);
        when(event1.getBlock()).thenReturn(block1);
        when(event1.getBlockPlaced()).thenReturn(blockPlaced1);
        lenient().when(block1.getType()).thenReturn(Material.RAIL);

        Location loc1 = new Location(world, 0, 64, 0);
        when(blockPlaced1.getLocation()).thenReturn(loc1);

        listener.onPlayerBlock(event1);

        // Given - Second Rail far away (distance 10 >= 8)
        BlockPlaceEvent event2 = mock(BlockPlaceEvent.class);
        Block block2 = mock(Block.class);
        Block blockPlaced2 = mock(Block.class);
        when(event2.getPlayer()).thenReturn(player);
        when(event2.getBlock()).thenReturn(block2);
        when(event2.getBlockPlaced()).thenReturn(blockPlaced2);
        lenient().when(block2.getType()).thenReturn(Material.RAIL);

        Location loc2 = new Location(world, 10, 64, 0);
        when(blockPlaced2.getLocation()).thenReturn(loc2);

        // Mock world for pathfinding
        when(world.getBlockAt(anyInt(), anyInt(), anyInt())).thenReturn(block);
        lenient().when(world.getBlockAt(any(Location.class))).thenReturn(block);
        when(block.getType()).thenReturn(Material.AIR);

        // Mock MaterialChecker
        lenient().when(materialChecker.isSolid(any())).thenReturn(false);
        lenient().when(materialChecker.isAir(any())).thenReturn(true);
        lenient().when(materialChecker.isWater(any())).thenReturn(false);
        lenient().when(materialChecker.isRail(any())).thenReturn(false);
        lenient().when(materialChecker.getMaxDurability(any())).thenReturn(0);
        lenient().when(materialChecker.isCompostable(any())).thenReturn(false);
        lenient().when(materialChecker.isEmpty(any())).thenReturn(true);

        // When
        listener.onPlayerBlock(event2);

        // Then
        verify(blockChanger, atLeast(10)).changeBlock(any(), any());
    }
}
