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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import coil.size.OriginalSize
import coil.size.Scale
import com.twitter.twittertext.Extractor
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.tweets.ReferencedTweetViewItem
import io.horizontalsystems.bankwallet.modules.coin.tweets.Tweet
import io.horizontalsystems.bankwallet.modules.coin.tweets.TweetViewItem
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme

@ExperimentalCoilApi
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

@ExperimentalCoilApi
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
        .background(ComposeAppTheme.colors.steel20)
        .padding(12.dp)
    ) {
        Text(
            text = referencedTweet.title.getString(),
            color = ComposeAppTheme.colors.grey,
            style = ComposeAppTheme.typography.micro
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = referencedTweet.text,
            color = ComposeAppTheme.colors.leah,
            style = ComposeAppTheme.typography.subhead2
        )
    }
}

@Composable
private fun TweetText(text: String, entities: List<Extractor.Entity>) {
    val spanStyles = entities.map {
        AnnotatedString.Range(
            SpanStyle(color = ComposeAppTheme.colors.issykBlue), it.start, it.end
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

@ExperimentalCoilApi
@Composable
private fun TweetTitle(tweet: TweetViewItem) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape),
            painter = rememberImagePainter(tweet.titleImageUrl),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = tweet.title,
                color = ComposeAppTheme.colors.oz,
                style = ComposeAppTheme.typography.body
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = tweet.subtitle,
                color = ComposeAppTheme.colors.grey,
                style = ComposeAppTheme.typography.caption
            )
        }
    }
}

@ExperimentalCoilApi
@Composable
private fun AttachmentPhoto(attachment: Tweet.Attachment.Photo) {
    Image(
        modifier = Modifier.fillMaxWidth(),
        painter = rememberImagePainter(
            attachment.url,
            builder = {
                size(OriginalSize)
                scale(Scale.FIT)
            },
        ),
        contentDescription = "",
        contentScale = ContentScale.FillWidth
    )
}

@ExperimentalCoilApi
@Composable
private fun AttachmentVideo(attachment: Tweet.Attachment.Video) {
    Box {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = rememberImagePainter(
                attachment.previewImageUrl,
                builder = {
                    size(OriginalSize)
                    scale(Scale.FIT)
                },
            ),
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
                ComposeAppTheme.colors.issykBlue
            } else {
                ComposeAppTheme.colors.steel20
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
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.caption
                    )
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 12.dp),
                        text = "${(proportion * 100).toInt()}%",
                        color = ComposeAppTheme.colors.leah,
                        style = ComposeAppTheme.typography.caption
                    )
                }
            }
        }
    }
}
