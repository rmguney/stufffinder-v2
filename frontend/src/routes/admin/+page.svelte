<script>
    import * as Card from "$lib/components/ui/card";
    import { onMount } from "svelte";
    import { goto } from "$app/navigation";
    import { browser } from "$app/environment";
    import { PUBLIC_API_URL } from "$env/static/public";

    let isAdmin = false;
    let reports = [];
    let currentPage = 0;
    let totalPages = 1;
    let loadingReports = true;
    let stats = {
        total: 0,
        byType: {},
    };
    let showResolveModal = false;
    let showDismissModal = false;
    let selectedReport = null;
    let resolutionNote = "";
    let removeContent = false;
    let banUser = false;
    let sendWarning = false;

    let filterStatus = "OPEN";
    let filterType = "";
    let filterReason = "";

    let statsOnlyOpen = true;

    $: filteredReports = reports.filter(
        (r) =>
            (!filterStatus || r.status === filterStatus) &&
            (!filterType || r.targetType === filterType) &&
            (!filterReason ||
                r.reason.toLowerCase().includes(filterReason.toLowerCase())),
    );

    async function fetchReports(page = 0) {
        loadingReports = true;
        try {
            const res = await fetch(
                `${PUBLIC_API_URL}/api/reports?page=${page}&size=10`,
            );
            const data = await res.json();

            reports = data.content;
            totalPages = data.page.totalPages;
            currentPage = data.page.number;

            // compute stats
            const reportsForStats = statsOnlyOpen
                ? reports.filter((r) => r.status === "OPEN")
                : reports;

            stats.total = reportsForStats.length;
            stats.byType = reportsForStats.reduce((acc, report) => {
                acc[report.targetType] = (acc[report.targetType] || 0) + 1;
                return acc;
            }, {});
        } catch (err) {
            console.error("Failed to fetch reports", err);
        } finally {
            loadingReports = false;
        }
    }

    async function resolveReport(action) {
        if (!selectedReport) return;
        try {
            await fetch(
                `${PUBLIC_API_URL}/api/reports/${selectedReport.id}/resolve`,
                {
                    method: "PUT",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({
                        action,
                        resolutionNote,
                        removeContent,
                        banUser,
                        sendWarning,
                    }),
                },
            );
            showResolveModal = false;
            showDismissModal = false;
            await fetchReports(currentPage);
        } catch (err) {
            console.error("Error resolving report", err);
        }
    }

    function viewTarget(report) {
        const id = report.targetId;
        const path =
            report.targetType === "POST"
                ? `/thread/${id}`
                : report.targetType === "COMMENT"
                  ? `/thread/${report.postId}`
                  : `/user/${report.reportedUsername}`;
        goto(path);
    }

    onMount(() => {
        if (browser) {
            const cookies = Object.fromEntries(
                document.cookie.split("; ").map((c) => c.split("=")),
            );
            isAdmin = cookies.userRole === "ADMIN";

            if (isAdmin) {
                fetchReports();
            } else {
                goto("/");
            }
        }
    });
</script>

<div
    class="min-h-screen flex justify-center bg-change dark:bg-dark shifting p-3 py-5"
>
    <div class="w-full max-w-5xl space-y-6">
        <!-- Dashboard Header -->
        <Card.Root
            class="overflow-hidden bg-white dark:bg-neutral-950 shadow-md rounded-md border border-neutral-200 dark:border-neutral-800"
        >
            <Card.Header class="p-6 text-center">
                <Card.Title
                    class="text-2xl md:text-3xl font-bold text-neutral-900 dark:text-white"
                >
                    Admin Dashboard
                </Card.Title>
                <Card.Description
                    class="text-neutral-600 dark:text-neutral-400 mt-1"
                >
                    Moderate reported content and manage flagged users.
                </Card.Description>
            </Card.Header>

            <!-- Summary -->
            <Card.Content class="px-6 pb-4">
                <div class="flex flex-wrap gap-6">
                    <div class="text-sm text-neutral-700 dark:text-neutral-300">
                        <span class="font-semibold">Total Reports:</span>
                        {stats.total}
                    </div>
                    {#each Object.entries(stats.byType) as [type, count]}
                        <div
                            class="text-sm text-neutral-700 dark:text-neutral-300"
                        >
                            <span class="font-semibold">{type}:</span>
                            {count}
                        </div>
                    {/each}
                    <label
                        class="text-sm text-neutral-700 dark:text-neutral-300 flex items-center gap-2"
                    >
                        <input
                            type="checkbox"
                            bind:checked={statsOnlyOpen}
                            on:change={() => fetchReports(currentPage)}
                        />
                        Show only OPEN reports in stats
                    </label>
                </div>
            </Card.Content>
        </Card.Root>

        <!-- Reports List -->
        <Card.Root
            class="bg-white dark:bg-neutral-950 rounded-md border border-neutral-200 dark:border-neutral-800 shadow"
        >
            <Card.Header class="px-6 pt-6">
                <Card.Title class="text-lg font-semibold"
                    >Reported Items</Card.Title
                >
            </Card.Header>
            <div class="flex gap-4 items-center px-6 pb-2 text-sm">
                <select bind:value={filterStatus}>
                    <option value="">All Statuses</option>
                    <option value="OPEN">Open</option>
                    <option value="RESOLVED">Resolved</option>
                    <option value="DISMISSED">Dismissed</option>
                </select>
                <select bind:value={filterType}>
                    <option value="">All Types</option>
                    <option value="POST">Post</option>
                    <option value="COMMENT">Comment</option>
                    <option value="PROFILE">Profile</option>
                </select>
                <input
                    type="text"
                    placeholder="Filter by reason..."
                    bind:value={filterReason}
                    class="border px-2 py-1 text-sm rounded"
                />
            </div>
            <Card.Content class="p-2 overflow-x-auto">
                {#if loadingReports}
                    <p class="text-sm text-neutral-500">Loading reports...</p>
                {:else if reports.length === 0}
                    <p class="text-sm text-neutral-500">No reports to show.</p>
                {:else}
                    <div class="min-w-[700px] sm:min-w-full">
                        <table
                            class="w-full text-sm table-auto border-collapse"
                        >
                            <thead>
                                <tr
                                    class="text-left text-neutral-600 dark:text-neutral-400 border-b border-neutral-200 dark:border-neutral-800"
                                >
                                    <th class="pb-2 px-3">Reporter</th>
                                    <th class="pb-2 px-3">Type</th>
                                    <th class="pb-2 px-3">Reason</th>
                                    <th class="pb-2 px-3">Notes</th>
                                    <th class="pb-2 px-3">Status</th>
                                    <th class="pb-2 px-3">Timestamp</th>
                                    <th class="pb-2 px-3">Resolved By</th>
                                    <th class="pb-2 px-3">Resolved At</th>
                                    <th class="pb-2 px-3">Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                {#each filteredReports as report}
                                    <tr
                                        class="border-t border-neutral-100 dark:border-neutral-800"
                                    >
                                        <td class="py-2 px-3"
                                            >{report.reporterUsername}</td
                                        >
                                        <td class="py-2 px-3"
                                            >{report.targetType}</td
                                        >
                                        <td class="py-2 px-3"
                                            >{report.reason}</td
                                        >
                                        <td
                                            class="py-2 text-neutral-600 dark:text-neutral-400 italic max-w-xs line-clamp-2 px-3"
                                        >
                                            {report.notes || "-"}
                                        </td>

                                        <td class="py-2 px-3"
                                            >{report.status}</td
                                        >
                                        <td class="py-2 px-3"
                                            >{new Date(
                                                report.createdAt,
                                            ).toLocaleString()}</td
                                        >
                                        <td class="py-2 px-3"
                                            >{report.resolvedByUsername ||
                                                "-"}</td
                                        >
                                        <td class="py-2 px-3">
                                            {report.resolvedAt
                                                ? new Date(
                                                      report.resolvedAt,
                                                  ).toLocaleString()
                                                : "-"}
                                        </td>

                                        <td class="py-2 px-3">
                                            <div class="flex flex-wrap gap-1">
                                                {#if report.status === "OPEN"}
                                                    <button
                                                        on:click={() => {
                                                            selectedReport =
                                                                report;
                                                            showResolveModal = true;
                                                        }}
                                                        class="text-xs px-2 py-1 rounded w-16 bg-emerald-600 text-white hover:bg-emerald-700"
                                                    >
                                                        Resolve
                                                    </button>
                                                    <button
                                                        on:click={() => {
                                                            selectedReport =
                                                                report;
                                                            showDismissModal = true;
                                                        }}
                                                        class="text-xs px-2 py-1 rounded w-16 bg-yellow-500 text-white hover:bg-yellow-600"
                                                    >
                                                        Dismiss
                                                    </button>
                                                {:else if report.status === "RESOLVED"}
                                                    <div
                                                        class="text-s text-neutral-600 dark:text-neutral-400"
                                                    >
                                                        Action taken:
                                                        {#if report.removeContent}<span
                                                                >Remove</span
                                                            >{/if}
                                                        {#if report.banUser}
                                                            <span
                                                                >{report.removeContent
                                                                    ? ", Ban"
                                                                    : "Ban"}</span
                                                            >
                                                        {/if}
                                                        {#if report.sendWarning}
                                                            <span>
                                                                {report.removeContent ||
                                                                report.banUser
                                                                    ? ", Warning"
                                                                    : "Warning"}
                                                            </span>
                                                        {/if}
                                                        {#if !report.removeContent && !report.banUser && !report.sendWarning}
                                                            <span>None</span>
                                                        {/if}
                                                    </div>
                                                {/if}
                                                {#if report.status !== "RESOLVED"}
                                                    <button
                                                        on:click={() =>
                                                            viewTarget(report)}
                                                        class="text-xs px-2 py-1 rounded w-16 bg-blue-600 text-white hover:bg-blue-700"
                                                    >
                                                        View
                                                    </button>
                                                {/if}
                                            </div>
                                        </td>
                                    </tr>
                                {/each}
                            </tbody>
                        </table>
                    </div>
                {/if}
            </Card.Content>

            <!-- Pagination -->
            {#if totalPages > 1}
                <div class="flex justify-center items-center py-4 gap-2">
                    <button
                        class="px-3 py-1 text-sm rounded border dark:border-neutral-700 border-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                        disabled={currentPage === 0}
                        on:click={() => fetchReports(currentPage - 1)}
                    >
                        Prev
                    </button>
                    <span
                        class="text-sm text-neutral-700 dark:text-neutral-300"
                    >
                        Page {currentPage + 1} of {totalPages}
                    </span>
                    <button
                        class="px-3 py-1 text-sm rounded border dark:border-neutral-700 border-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-800"
                        disabled={currentPage + 1 >= totalPages}
                        on:click={() => fetchReports(currentPage + 1)}
                    >
                        Next
                    </button>
                </div>
            {/if}
        </Card.Root>
    </div>
    {#if showResolveModal}
        <div
            class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        >
            <div
                class="bg-white dark:bg-neutral-950 rounded-lg shadow-xl w-full max-w-lg p-6 border border-neutral-200 dark:border-neutral-800"
            >
                <h2 class="text-lg font-semibold mb-4">Resolve Report</h2>

                <!-- svelte-ignore a11y-label-has-associated-control -->
                <label class="block text-sm font-medium mb-1"
                    >Resolution Note</label
                >
                <textarea
                    bind:value={resolutionNote}
                    rows="3"
                    class="w-full p-2 mb-4 text-sm rounded border border-neutral-300 dark:border-neutral-700 dark:bg-neutral-800"
                />

                <div class="space-y-2 mb-4 text-sm">
                    {#if selectedReport && selectedReport.targetType !== "PROFILE"}
                        <label>
                            <input
                                type="checkbox"
                                bind:checked={removeContent}
                            />
                            Remove content
                        </label>
                    {/if}

                    {#if selectedReport && selectedReport.targetType === "PROFILE"}
                        <label>
                            <input type="checkbox" bind:checked={banUser} />
                            Ban user
                        </label>
                    {/if}

                    <label>
                        <input type="checkbox" bind:checked={sendWarning} />
                        Send warning
                    </label>
                </div>

                <div class="flex justify-end gap-2">
                    <button
                        on:click={() => {
                            showResolveModal = false;
                            resolutionNote = "";
                            removeContent = false;
                            banUser = false;
                            sendWarning = false;
                        }}
                        class="px-3 py-1 rounded text-sm border border-neutral-300 dark:border-neutral-700"
                    >
                        Cancel
                    </button>
                    <button
                        on:click={() => resolveReport("RESOLVED")}
                        class="px-3 py-1 rounded text-sm bg-emerald-600 text-white hover:bg-emerald-700"
                    >
                        Resolve
                    </button>
                </div>
            </div>
        </div>
    {/if}
    {#if showDismissModal}
        <div
            class="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4"
        >
            <div
                class="bg-white dark:bg-neutral-950 rounded-lg shadow-xl w-full max-w-lg p-6 border border-neutral-200 dark:border-neutral-800"
            >
                <h2 class="text-lg font-semibold mb-4">Dismiss Report</h2>

                <!-- svelte-ignore a11y-label-has-associated-control -->
                <label class="block text-sm font-medium mb-1"
                    >Resolution Note</label
                >
                <textarea
                    bind:value={resolutionNote}
                    rows="3"
                    class="w-full p-2 mb-4 text-sm rounded border border-neutral-300 dark:border-neutral-700 dark:bg-neutral-800"
                />

                <div class="flex justify-end gap-2">
                    <button
                        on:click={() => {
                            showDismissModal = false;
                            resolutionNote = "";
                        }}
                        class="px-3 py-1 rounded text-sm border border-neutral-300 dark:border-neutral-700"
                    >
                        Cancel
                    </button>
                    <button
                        on:click={() => {
                            resolveReport("DISMISSED");
                            showDismissModal = false;
                        }}
                        class="px-3 py-1 rounded text-sm bg-yellow-500 text-white hover:bg-yellow-600"
                    >
                        Dismiss
                    </button>
                </div>
            </div>
        </div>
    {/if}
</div>
