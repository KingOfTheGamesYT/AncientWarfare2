package net.shadowmage.ancientwarfare.automation.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.shadowmage.ancientwarfare.core.api.AWItems;
import net.shadowmage.ancientwarfare.core.item.ItemHammer;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class AWAutomationItemLoader
{

public static final CreativeTabs automationTab = new CreativeTabs("tabs.automation")
  {
  @Override
  @SideOnly(Side.CLIENT)
  public Item getTabIconItem()
    {  
    return AWItems.automationHammer;
    }  
  };
 
public static void load()
  {
  }

}
