interface Season {
    id: number;
    start_week: number;
    end_week: number;
    initial_coins: number;
}

interface User {
    id: number;
    username: string;
    coins: number;
}

interface Game {
    id: number;
    home_team: string;
    away_team: string;
    nfl_week: number;
    commence_time: string;
    winner?: 'home' | 'away';
}

interface Bet {
    id: number;
    username: string;
    season_id: number;
    bet_type: string;
    amount: number;
}

interface NewSeason {
    startWeek: number;
    endWeek: number;
    initialCoins: number;
}

function dashboardData() {
    return {
        seasons: [] as Season[],
        users: [] as User[],
        pendingGames: [] as Game[],
        unsettledBets: [] as Bet[],
        selectedSeason: '',
        selectedGameId: '',
        updateResult: '',
        errorMessage: '',
        newSeason: {
            startWeek: 0,
            endWeek: 0,
            initialCoins: 0
        } as NewSeason,

        init(): void {
            console.log('Initializing dashboard...');
            this.loadSeasons();
        },

        async fetchWithErrorHandling(url: string, options: RequestInit = {}): Promise<any> {
            try {
                const response = await fetch(url, options);
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return await response.json();
            } catch (error) {
                console.error('Fetch error:', error);
                this.errorMessage = `Error: ${error.message}`;
                throw error;
            }
        },

        async loadSeasons(): Promise<void> {
            console.log('Loading seasons...');
            try {
                this.seasons = await this.fetchWithErrorHandling('/api/seasons');
                console.log('Seasons loaded:', this.seasons);
            } catch (error) {
                console.error('Error loading seasons:', error);
            }
        },

        async createSeason(): Promise<void> {
            console.log('Creating season:', this.newSeason);
            try {
                await this.fetchWithErrorHandling('/api/seasons', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify(this.newSeason)
                });
                console.log('Season created successfully');
                await this.loadSeasons();
                this.newSeason = { startWeek: 0, endWeek: 0, initialCoins: 0 };
            } catch (error) {
                console.error('Error creating season:', error);
            }
        },

        async deleteSeason(seasonId: number): Promise<void> {
            console.log('Deleting season:', seasonId);
            try {
                await this.fetchWithErrorHandling(`/api/seasons/${seasonId}`, { method: 'DELETE' });
                console.log('Season deleted successfully');
                await this.loadSeasons();
            } catch (error) {
                console.error('Error deleting season:', error);
            }
        },

        async loadUsers(): Promise<void> {
            if (this.selectedSeason) {
                console.log('Loading users for season:', this.selectedSeason);
                try {
                    this.users = await this.fetchWithErrorHandling(`/api/users/seasons/${this.selectedSeason}`);
                    console.log('Users loaded:', this.users);
                } catch (error) {
                    console.error('Error loading users:', error);
                }
            }
        },

        async loadPendingGames(): Promise<void> {
            console.log('Loading pending games...');
            try {
                this.pendingGames = await this.fetchWithErrorHandling('/api/nfl/games/pending-results');
                console.log('Pending games loaded:', this.pendingGames);
            } catch (error) {
                console.error('Error loading pending games:', error);
            }
        },

        async updateGameResult(gameId: number, winner: 'home' | 'away'): Promise<void> {
            console.log('Updating game result:', gameId, winner);
            try {
                await this.fetchWithErrorHandling(`/api/nfl/games/${gameId}/result`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: `winner=${winner}`
                });
                console.log('Game result updated successfully');
                await this.loadPendingGames();
            } catch (error) {
                console.error('Error updating game result:', error);
            }
        },

        async loadUnsettledBets(): Promise<void> {
            if (this.selectedGameId) {
                console.log('Loading unsettled bets for game:', this.selectedGameId);
                try {
                    this.unsettledBets = await this.fetchWithErrorHandling(`/api/bets/unsettled/${this.selectedGameId}`);
                    console.log('Unsettled bets loaded:', this.unsettledBets);
                } catch (error) {
                    console.error('Error loading unsettled bets:', error);
                }
            }
        },

        async updateDatabase(): Promise<void> {
            console.log('Updating database...');
            try {
                const response = await fetch('/api/admin/update-database', { method: 'POST' });
                this.updateResult = await response.text();
                console.log('Database update result:', this.updateResult);
            } catch (error) {
                console.error('Error updating database:', error);
                this.updateResult = 'Error updating database';
            }
        },

        formatDate(dateString: string): string {
            return new Date(dateString).toLocaleString();
        }
    };
}