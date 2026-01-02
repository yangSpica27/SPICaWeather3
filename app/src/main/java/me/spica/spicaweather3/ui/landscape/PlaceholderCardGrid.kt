package me.spica.spicaweather3.ui.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 占位卡片类型定义
 */
data class PlaceholderCardInfo(
    val type: String,
    val title: String,
    val description: String,
    val spanCount: Int = 1  // 占用的列数
)

/**
 * 获取占位卡片列表（使用 lazy 避免每次都创建）
 */
private val placeholderCards: List<PlaceholderCardInfo> = listOf(
    PlaceholderCardInfo("now", "实时天气", "当前温度、天气状况", spanCount = 2),
    PlaceholderCardInfo("hourly", "逐小时预报", "未来24小时天气"),
    PlaceholderCardInfo("daily", "逐日预报", "未来7天天气"),
    PlaceholderCardInfo("aqi", "空气质量", "AQI指数详情"),
    PlaceholderCardInfo("uv", "紫外线", "UV指数"),
    PlaceholderCardInfo("lifestyle", "生活指数", "穿衣、运动建议"),
    PlaceholderCardInfo("wind", "风力风向", "风速风向详情"),
    PlaceholderCardInfo("humidity", "湿度", "相对湿度"),
)

/**
 * 占位卡片网格
 */
@Composable
fun PlaceholderCardGrid(
    cityState: WeatherPageState,
    onCardClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // 标题
        Text(
            text = cityState.cityEntity.name,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        // 副标题 - 天气描述
        cityState.cityEntity.weather?.todayWeather?.let { today ->
            Text(
                text = "${today.temp}°",
                fontSize = 16.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 卡片网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = placeholderCards,
                key = { it.type },
                span = { card -> GridItemSpan(card.spanCount) }
            ) { card ->
                PlaceholderCard(
                    cardInfo = card,
                    onClick = { onCardClick(card.type) }
                )
            }
        }
    }
}

/**
 * 单个占位卡片
 */
@Composable
private fun PlaceholderCard(
    cardInfo: PlaceholderCardInfo,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (cardInfo.spanCount > 1) 160.dp else 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = cardInfo.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )
            
            Text(
                text = cardInfo.description,
                fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary
            )
            
            // 占位指示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MiuixTheme.colorScheme.outline.copy(alpha = 0.2f))
            )
        }
    }
}

/**
 * 卡片详情占位页
 */
@Composable
fun CardDetailPlaceholder(
    cardType: String,
    cityState: WeatherPageState,
    onBack: () -> Unit
) {
    val cardInfo = remember(cardType) {
        placeholderCards.find { it.type == cardType }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = MiuixTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = cardInfo?.title ?: cardType,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
        
        // 详情内容占位
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 占位图标区域
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cardType.uppercase(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "${cardInfo?.title ?: cardType} 详情",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "城市: ${cityState.cityEntity.name}",
                    fontSize = 16.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
                
                Text(
                    text = "此处为详情内容占位\n后续将显示完整的${cardInfo?.title ?: cardType}信息",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}
