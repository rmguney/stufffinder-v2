<script>
  import Post from '$lib/components/post.svelte';
  import { threadStore, forceRefreshThreads } from '../../threadStore';
  import { onMount } from 'svelte';
  import Pagination from '$lib/components/ui/pagination/pagination.svelte';

  export let sortBy = 'none';

  let currentPage = 1;
  const postsPerPage = 9;
  let allPosts = [];

  $: {
    allPosts = $threadStore.map(post => ({
      ...post,
      createdAt: post.createdAt,
      tags: [...(post.tags || [])],
      mediaFiles: [...(post.mediaFiles || [])],
      mysteryObject: post.mysteryObject ? { ...post.mysteryObject } : null
    }));
    
    console.log("Tüm postların tarihleri:");
    allPosts.forEach(post => {
      console.log(`Post ID: ${post.id}, Title: ${post.title}, Created At: ${post.createdAt}, Date Object: ${new Date(post.createdAt)}`);
    });
  }

  $: filteredPosts = allPosts.filter(post => {
    if (sortBy === 'resolved') return post.solved === true;
    if (sortBy === 'unresolved') return post.solved === false;
    return true;
  });

  $: sortedPosts = filteredPosts.sort((a, b) => {
    const dateA = new Date(a.createdAt).getTime();
    const dateB = new Date(b.createdAt).getTime();
    
    switch (sortBy) {
      case 'recent':
        if (dateA === dateB) return b.id - a.id;
        return dateB - dateA;
      case 'oldest':
        if (dateA === dateB) return a.id - b.id;
        return dateA - dateB;
      case 'trending':
        const scoreA = (a.upvotesCount || 0) - (a.downvotesCount || 0);
        const scoreB = (b.upvotesCount || 0) - (b.downvotesCount || 0);
        if (scoreA === scoreB) {
          if (dateA === dateB) return b.id - a.id;
          return dateB - dateA;
        }
        return scoreB - scoreA;
      default:
        if (dateA === dateB) return b.id - a.id;
        return dateB - dateA;
    }
  });

  $: totalPages = Math.ceil(sortedPosts.length / postsPerPage);
  $: startIndex = (currentPage - 1) * postsPerPage;
  $: endIndex = startIndex + postsPerPage;
  $: displayPosts = sortedPosts.slice(startIndex, endIndex);

  $: if (sortBy) {
    currentPage = 1;
  }

  function handlePageChange(newPage) {
    if (newPage >= 1 && newPage <= totalPages) {
      currentPage = newPage;
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }

  onMount(async function () {
    console.log("Thread data sample:", $threadStore.length > 0 ? $threadStore[0] : "No threads");
  });
</script>

<div class="flex flex-col gap-6 w-full">
  <div class="grid grid-cols-1 lg:grid-cols-3 gap-4 lg:gap-6">
    {#each displayPosts as thread (thread.id)}
      <div class="w-full">
        <a href={`/thread/${thread.id}`} class="block h-full">
          <Post
            id={thread.id}
            title={thread.title}
            description={thread.description || ""}
            tags={thread.tags || []}
            imageSrc={thread.mysteryObjectImageUrl ? thread.mysteryObjectImageUrl : ''}
            mediaFiles={thread.mediaFiles || []}
            postedBy={thread.author}
            createdAt={thread.createdAt}
            updatedAt={thread.updatedAt}
            upvotes={thread.upvotesCount || 0}
            downvotes={thread.downvotesCount || 0}
            commentCount={thread.commentCount || 0}
            userUpvoted={thread.userUpvoted || false}
            userDownvoted={thread.userDownvoted || false}
            solved={thread.solved}
            mysteryObject={thread.mysteryObject || null}
            variant="thumb"
          />
        </a>
      </div>
    {:else}
      <div class="col-span-full text-center py-8">
        <p>No posts found. Check back later!</p>
      </div>
    {/each}
  </div>

  {#if totalPages > 1}
    <div class="w-full flex justify-center mt-6">
      <Pagination 
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={handlePageChange}
      />
    </div>
  {/if}
</div>