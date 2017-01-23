package powercrystals.minefactoryreloaded.render.item;

import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCOBJParser;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.texture.TextureUtils;
import codechicken.lib.util.TransformUtils;
import codechicken.lib.vec.SwapYZ;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

import javax.vecmath.Matrix4f;
import java.util.Map;

public class NeedleGunItemRenderer extends BaseItemRenderer {

	public static CCModel gunModel;
	public static CCModel magazineModel;
	private static boolean initialized = false;
	private static ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> TRANSFORMATIONS;
	
	@Override
	public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {

		return MapWrapper.handlePerspective(this, TRANSFORMATIONS, cameraTransformType);
	}

	public static void loadModel() {
		
		if (initialized) {
			return;
		}
		initialized = true;
		Map<String, CCModel> models = CCOBJParser.parseObjModels(new ResourceLocation("minefactoryreloaded", "models/needle_gun.obj"), new SwapYZ());
		gunModel = models.get("gun");
		magazineModel = models.get("magazine");

		TRSRTransformation thirdPerson = TransformUtils.get(0, 0, 0, 90, 180, 0, 0.025f);
		ImmutableMap.Builder<ItemCameraTransforms.TransformType, TRSRTransformation> builder = ImmutableMap.builder();
		builder.put(ItemCameraTransforms.TransformType.GUI, TransformUtils.get(-3, -1, 0, 30, 135, 0, 0.02f));
		builder.put(ItemCameraTransforms.TransformType.GROUND, TransformUtils.get(0, 3, 0, 0, 0, 0, 0.02f));
		builder.put(ItemCameraTransforms.TransformType.FIXED, TransformUtils.get(-5, 0, 0, 0, 90, 0, 0.03f));
		builder.put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND, thirdPerson);
		builder.put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND, TransformUtils.leftify(thirdPerson));
		builder.put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND, TransformUtils.get(4, 0, 0, 8, 190, 0, 0.025f));
		builder.put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, TransformUtils.get(4, 0, 0, 8, 190, 0, 0.025f));
		TRANSFORMATIONS = builder.build();
	}
	
	protected void drawModel(CCRenderState ccrs, ItemStack stack) {

		TextureUtils.changeTexture("minefactoryreloaded:textures/itemmodels/needle_gun.png");
		ccrs.startDrawing(4, DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL);
		
		gunModel.render(ccrs);
		if (stack.getTagCompound() != null && stack.getTagCompound().hasKey("ammo") &&
				!stack.getTagCompound().getCompoundTag("ammo").hasNoTags()) {
			magazineModel.render(ccrs);
		}

		ccrs.draw();
	}
}
