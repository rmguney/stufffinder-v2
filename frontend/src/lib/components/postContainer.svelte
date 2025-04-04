<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore, forceRefreshThreads } from '../../threadStore';
  import { onMount } from 'svelte';

  onMount(async function () {
    console.log("Thread data sample:", $threadStore.length > 0 ? $threadStore[0] : "No threads");
  });
</script>

{#each $threadStore.slice().sort((a, b) => b.id - a.id) as thread}
  <div class="w-full lg:w-[calc(33.333%-1rem)]">
    <a href={`/thread/${thread.id}`}>
      <Post
        id={thread.id}
        title={thread.title}
        description={thread.description || ""}
        tags={thread.tags || []}
        imageSrc={thread.mysteryObjectImage ? `data:image/png;base64,${thread.mysteryObjectImage}` : ''}
        mediaFiles={thread.mediaFiles || []}
        postedBy={thread.author}
        createdAt={thread.createdAt}
        updatedAt={thread.updatedAt}
        upvotes={thread.upvotes || 0}
        downvotes={thread.downvotes || 0}
        userUpvoted={thread.userUpvoted || false}
        userDownvoted={thread.userDownvoted || false}
        solved={thread.solved}
        mysteryObject={thread.mysteryObject || null}
        variant="thumb"
      />
    </a>
  </div>
{:else}
  <div class="w-full text-center py-8">
    <p>No posts found. Check back later!</p>
  </div>
{/each}