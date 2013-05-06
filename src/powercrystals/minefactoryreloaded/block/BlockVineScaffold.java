package powercrystals.minefactoryreloaded.block;

import powercrystals.core.position.BlockPosition;
import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class BlockVineScaffold extends Block
{
	private Icon _sideIcon;
	private Icon _topIcon;
	
	private static final ForgeDirection[] _attachDirections = new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.EAST, ForgeDirection.WEST };
	private static final int _attachDistance = 16;

	public BlockVineScaffold(int id)
	{
		super(id, Material.leaves);
		setUnlocalizedName("mfr.vinescaffold");
		setStepSound(soundGrassFootstep);
		setHardness(0.1F);
		setBlockBounds(0F, 0.01F, 0F, 1F, 0.99F, 1F);
	}

	@Override
	public void onEntityCollidedWithBlock(World par1World, int par2, int par3, int par4, Entity par5Entity)
	{
		if(par5Entity instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)par5Entity;
			if(player.isSneaking())
			{
				player.motionY = 0;
			}
			else if(Math.abs(player.motionX) + Math.abs(player.motionZ) > 0.001)
			{
				player.motionY = 0.2D;
			}
			else
			{
				player.motionY = -0.15D;
			}
			player.fallDistance = 0;
		}
	}
	
	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z)
	{
		return AxisAlignedBB.getAABBPool().getAABB(x + 0.05, y, z + 0.05, x + 0.95, y + 1, z + 0.95);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IconRegister ir)
	{
		_sideIcon = ir.registerIcon("powercrystals/minefactoryreloaded/" + getUnlocalizedName() + ".side");
		_topIcon = ir.registerIcon("powercrystals/minefactoryreloaded/" + getUnlocalizedName() + ".top");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Icon getIcon(int side, int meta)
	{
		return side < 2 ? _topIcon : _sideIcon;
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}
	
	@Override
	public int getRenderType()
	{
		return MineFactoryReloadedCore.renderIdVineScaffold;
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side)
	{
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderColor(int meta)
	{
		return ColorizerFoliage.getFoliageColorBasic();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int colorMultiplier(IBlockAccess world, int x, int y, int z)
	{
		int r = 0;
		int g = 0;
		int b = 0;

		for(int zOffset = -1; zOffset <= 1; ++zOffset)
		{
			for(int xOffset = -1; xOffset <= 1; ++xOffset)
			{
				int biomeColor = world.getBiomeGenForCoords(x + xOffset, z + zOffset).getBiomeFoliageColor();
				r += (biomeColor & 16711680) >> 16;
				g += (biomeColor & 65280) >> 8;
				b += biomeColor & 255;
			}
		}

		return (r / 9 & 255) << 16 | (g / 9 & 255) << 8 | b / 9 & 255;
	}
	
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float xOffset, float yOffset, float zOffset)
	{
		if(!world.isRemote && player.inventory.mainInventory[player.inventory.currentItem] != null && player.inventory.mainInventory[player.inventory.currentItem].itemID == blockID)
		{
			for(int i = y; i < 256; i++)
			{
				if(world.isAirBlock(x, i, z))
				{
					world.setBlock(x, i, z, blockID, 0, 3);
					player.inventory.mainInventory[player.inventory.currentItem].stackSize--;
					if(player.inventory.mainInventory[player.inventory.currentItem].stackSize == 0)
					{
						player.inventory.mainInventory[player.inventory.currentItem] = null;
					}
					break;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean canPlaceBlockAt(World world, int x, int y, int z)
	{
		return canBlockStay(world, x, y, z);
	}
	
	@Override
	public boolean canBlockStay(World world, int x, int y, int z)
	{
		if(world.isBlockSolidOnSide(x, y - 1, z, ForgeDirection.UP))
		{
			return true;
		}
		for(ForgeDirection d : _attachDirections)
		{
			BlockPosition bp = new BlockPosition(x, y, z, d);
			for(int i = 0; i < _attachDistance; i++)
			{
				bp.moveForwards(1);
				if(world.getBlockId(bp.x, bp.y, bp.z) == blockID && world.isBlockSolidOnSide(bp.x, bp.y - 1, bp.z, ForgeDirection.UP))
				{
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int side)
	{
		if(!canBlockStay(world, x, y, z))
		{
			dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
			world.setBlockToAir(x, y, z);
		}
	}
	
	@Override
	public boolean isBlockSolidOnSide(World world, int x, int y, int z, ForgeDirection side)
	{
		return (side == ForgeDirection.UP || side == ForgeDirection.DOWN) ? true : false;
	}
}