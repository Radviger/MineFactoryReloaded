package powercrystals.minefactoryreloaded.tile.machine.animals;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import powercrystals.minefactoryreloaded.block.BlockFactoryMachine;
import powercrystals.minefactoryreloaded.core.Area;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryInventory;
import powercrystals.minefactoryreloaded.gui.client.GuiFactoryPowered;
import powercrystals.minefactoryreloaded.gui.container.ContainerFisher;
import powercrystals.minefactoryreloaded.setup.MFRConfig;
import powercrystals.minefactoryreloaded.setup.Machine;
import powercrystals.minefactoryreloaded.tile.base.TileEntityFactoryPowered;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

public class TileEntityFisher extends TileEntityFactoryPowered {

	private static final int WORK_BASE = 1800;

	private boolean _isJammed = true, _needItem = false;
	private byte _speed, _luck;
	private int _workNeeded = WORK_BASE, boost;
	protected Random _rand = null;

	public TileEntityFisher() {

		super(Machine.Fisher);
		createHAM(this, 1);
		setManageSolids(true);
	}

	@Override
	public void onLoad() {

		super.onLoad();

		if (_rand == null) {
			_rand = new Random(world.getSeed() ^ world.rand.nextLong());
		}
		validateLocation();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiFactoryInventory getGui(InventoryPlayer inventoryPlayer) {

		return new GuiFactoryPowered(getContainer(inventoryPlayer), this);
	}

	@Override
	public ContainerFisher getContainer(InventoryPlayer inventoryPlayer) {

		return new ContainerFisher(this, inventoryPlayer);
	}

	@Override
	public EnumFacing getDirectionFacing() {

		return EnumFacing.DOWN;
	}

	@Override
	public boolean activateMachine() {

		if (_isJammed || world.getTotalWorldTime() % 137 == 0) {
			validateLocation();
		}
		if (_isJammed || _needItem)
			return false;

		if (!incrementWorkDone()) return false;

		if (getWorkDone() > getWorkMax()) {
			_inventory.get(0).attemptDamageItem(1, _rand, null);
			LootContext.Builder context = new LootContext.Builder((WorldServer)this.world);
			context.withLuck(_luck);
			for (@Nonnull ItemStack stack : this.world.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING).generateLootForPools(_rand, context.build())) {
				if (_inventory.get(0).isEmpty() && isItemValidForSlot(0, stack)) {
					setInventorySlotContents(0, stack);
				} else {
					doDrop(stack);
				}
			}
			setWorkDone(0);
		}
		return true;
	}

	protected void validateLocation() {

		Area fishingHole = _areaManager.getHarvestArea();
		int extraBlocks = 0;
		int xCoord = pos.getX(), zCoord = pos.getZ();
		for (BlockPos bp : fishingHole.getPositionsBottomFirst()) {
			int x = bp.getX(), y = bp.getY(), z = bp.getZ();

			if (!isValidBlock(x, y, z)) {
				_isJammed = true;
				setIdleTicks(getIdleTicksMax());
				return;
			} else if (isValidBlock(x, y - 1, z)) {
				++extraBlocks;
			}
			if ((x != xCoord) || z != zCoord) {
				int xMod = x - (xCoord - x), zMod = z - (zCoord - z);

				if (isValidBlock(xMod, y, zMod)) {
					++extraBlocks;
				} else if (isFisher(xMod, y, zMod))
					extraBlocks -= 18;
				if (isFisher(xMod, y, z))
					extraBlocks -= 18;
				if (isFisher(x, y, zMod))
					extraBlocks -= 18;

			} else if (isValidBlock(x, y - 2, z)) {
				++extraBlocks;
			}
		}
		_workNeeded = WORK_BASE - extraBlocks * 50;
		_isJammed = false;
	}

	protected boolean isFisher(int x, int y, int z) {

		if (y == pos.getY() - 1 && !(x == pos.getX() && z == pos.getZ())) {
			IBlockState state = world.getBlockState(new BlockPos(x, pos.getY(), z));
			if (state.getBlock() == _machine.getBlock()
					&& state.getValue(BlockFactoryMachine.TYPE) == BlockFactoryMachine.Type.FISHER)
				return true;
		}
		return false;
	}

	protected boolean isValidBlock(int x, int y, int z) {

		BlockPos loc = new BlockPos(x, y, z);
		if (!world.isBlockLoaded(loc) || isFisher(x, y, z))
			return false;
		IBlockState state = world.getBlockState(loc);
		Block block = state.getBlock();
		if (block.isAssociatedBlock(Blocks.WATER) || block.isAssociatedBlock(Blocks.FLOWING_WATER)) {
			return state.getValue(BlockFluidBase.LEVEL) == 0;
		}
		return false;
	}

	@Override
	public EnumFacing getDropDirection() {

		return EnumFacing.UP;
	}

	@Override
	public int getWorkMax() {

		return _workNeeded - boost;
	}

	@SideOnly(Side.CLIENT)
	public void setWorkMax(int work) {

		_workNeeded = work;
	}

	@Override
	protected void onFactoryInventoryChanged() {

		super.onFactoryInventoryChanged();

		boost = 0;
		_needItem = false;
		if ((world == null || !world.isRemote) && !_inventory.get(0).isEmpty()) {
			_luck = (byte) EnchantmentHelper.getEnchantmentLevel(Enchantments.LUCK_OF_THE_SEA, _inventory.get(0));
			_speed = (byte) EnchantmentHelper.getEnchantmentLevel(Enchantments.LURE, _inventory.get(0));
			boost = 75 * _speed + 75;
		} else {
			_needItem = MFRConfig.fisherNeedsRod.getBoolean(false);
			if (_needItem) {
				setIdleTicks(getIdleTicksMax());
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {

		super.readFromNBT(tag);
		if (tag.hasKey("workNeeded"))
			_workNeeded = tag.getInteger("workNeeded");
		if (tag.hasKey("jam"))
			_isJammed = tag.getBoolean("jam");
		if (tag.hasKey("seed"))
			_rand = new Random(tag.getLong("seed"));
	}

	private static Field _Random_seed = null;
	static {
		try {
			Field f = Random.class.getDeclaredField("seed");
			f.setAccessible(true);
			_Random_seed = f;
		} catch (Throwable t) {
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {

		tag = super.writeToNBT(tag);
		tag.setInteger("workNeeded", _workNeeded);
		tag.setBoolean("jam", _isJammed);
		if (_Random_seed != null)
			try {
				tag.setLong("seed", ((AtomicLong) _Random_seed.get(_rand)).get());
			} catch (Throwable e) {
			}
		return tag;
	}

	@Override
	public int getIdleTicksMax() {

		return 200;
	}

	@Override
	public boolean isItemValidForSlot(int slot, @Nonnull ItemStack stack) {

		return stack.isEmpty() || (stack.isItemStackDamageable() && stack.getItem().isEnchantable(stack) && Enchantments.LURE.canApply(stack));
	}

	@Override
	public int getSizeInventory() {

		return 1;
	}

}