package io.horizontalsystems.bankwallet.ui.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.twitter.twittertext.Extractor
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.tweets.ReferencedTweetViewItem
import io.horizontalsystems.bankwallet.modules.coin.tweets.Tweet
import io.horizontalsystems.bankwallet.modules.coin.tweets.TweetViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@Preview
@Composable
fun CellTweetPreview() {
    ComposeAppTheme {
        CellTweet(TweetViewItem(
            title = "Super",
            subtitle = "@super",
            titleImageUrl = "",
            text = "Some special!!! Unbelievable...",
            attachments = listOf(),
            date = "Nov 12, 12:39",
            referencedTweet = null,
            entities = listOf(),
            url = ""
        )) {

        }
    }
}

@Composable
fun CellTweet(tweet: TweetViewItem, onClick: (TweetViewItem) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ComposeAppTheme.colors.lawrence)
            .clickable {
                onClick.invoke(tweet)
            }
            .padding(16.dp)
    ) {
        TweetTitle(tweet)

        Spacer(modifier = Modifier.height(12.dp))
        TweetText(tweet.text, tweet.entities)

        tweet.attachments.forEach { attachment ->
            Spacer(modifier = Modifier.height(12.dp))
            when (attachment) {
                is Tweet.Attachment.Photo -> AttachmentPhoto(attachment)
                is Tweet.Attachment.Poll -> AttachmentPoll(attachment)
                is Tweet.Attachment.Video -> AttachmentVideo(attachment)
            }
        }

        tweet.referencedTweet?.let { referencedTweet ->
            Spacer(modifier = Modifier.height(12.dp))
            TweetReferencedTweet(referencedTweet)
        }

        Spacer(modifier = Modifier.height(12.dp))
        TweetDate(tweet)
    }
}

@Composable
private fun TweetDate(tweet: TweetViewItem) {
    Text(
        text = tweet.date,
        color = ComposeAppTheme.colors.grey,
        style = ComposeAppTheme.typography.micro
    )
}

@Composable
private fun TweetReferencedTweet(referencedTweet: ReferencedTweetViewItem) {
    Column(Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ComposeAppTheme.colors.steel10)
        .padding(12.dp)
    ) {
        Text(
            text = referencedTweet.title.getString(),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.micro
        )
        Spacer(modifier = Modifier.height(12.dp))
        subhead2_leah(text = referencedTweet.text)
    }
}

@Composable
private fun TweetText(text: String, entities: List<Extractor.Entity>) {
    val spanStyles = entities.map {
        AnnotatedString.Range(
            SpanStyle(color = ComposeAppTheme.colors.laguna), it.start, it.end
        )
    }
    Text(
        text = AnnotatedString(
            text = text,
            spanStyles = spanStyles,
        ),
        color = ComposeAppTheme.colors.leah,
        style = ComposeAppTheme.typography.subhead2
    )
}

@Composable
private fun TweetTitle(tweet: TweetViewItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
            painter = rememberAsyncImagePainter(tweet.titleImageUrl),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            body_leah(text = tweet.title)
            Spacer(modifier = Modifier.height(3.dp))
            caption_grey(text = tweet.subtitle)
        }
    }
}

@Composable
private fun AttachmentPhoto(attachment: Tweet.Attachment.Photo) {
    val model = ImageRequest.Builder(LocalContext.current)
        .data(attachment.url)
        .size(Size.ORIGINAL)
        .crossfade(true)
        .build()
    Image(
        modifier = Modifier.fillMaxWidth(),
        painter = rememberAsyncImagePainter(model),
        contentDescription = "",
        contentScale = ContentScale.FillWidth
    )
}

@Composable
private fun AttachmentVideo(attachment: Tweet.Attachment.Video) {
    Box {
        val model = ImageRequest.Builder(LocalContext.current)
            .data(attachment.previewImageUrl)
            .size(Size.ORIGINAL)
            .crossfade(true)
            .build()
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberAsyncImagePainter(model),
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .background(ComposeAppTheme.colors.black50)
        )

        Icon(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = R.drawable.play_48),
            contentDescription = null,
            tint = ComposeAppTheme.colors.white,
        )
    }
}

@Composable
private fun AttachmentPoll(attachment: Tweet.Attachment.Poll) {
    val totalVotes = attachment.options.sumOf { it.votes }
    val maxVotes = attachment.options.maxOf { it.votes }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attachment.options.forEach { option ->
            val proportion = option.votes / totalVotes.toFloat()
            val color = if (option.votes == maxVotes) {
                ComposeAppTheme.colors.laguna
            } else {
                ComposeAppTheme.colors.steel20
            }
            val textColor = if (option.votes == maxVotes) {
                ComposeAppTheme.colors.claude
            } else {
                ComposeAppTheme.colors.leah
            }
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(ComposeAppTheme.colors.steel10)
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(proportion)
                        .clip(RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50))
                        .background(color)
                )
                Row(
                    modifier = Modifier.matchParentSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .weight(1f),
                        text = option.label,
                        color = textColor,
                        style = ComposeAppTheme.typography.caption
                    )
                    caption_leah(
                        modifier = Modifier
                            .padding(horizontal = 12.dp),
                        text = "${(proportion * 100).toInt()}%",
                    )
                }
            }
        }
    }
}
